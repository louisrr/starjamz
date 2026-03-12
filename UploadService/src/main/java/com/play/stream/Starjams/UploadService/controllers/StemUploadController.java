package com.play.stream.Starjams.UploadService.controllers;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.Upload;
import com.play.stream.Starjams.UploadService.dto.StemBundleUploadResponse;
import com.play.stream.Starjams.UploadService.dto.StemSeparationResponse;
import com.play.stream.Starjams.UploadService.services.DemucsClient;
import com.play.stream.Starjams.UploadService.services.StemMetadataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Handles stem bundle uploads (.stems ZIP files) and flat audio uploads
 * that need automatic Demucs separation.
 *
 * POST /api/upload/stems          — upload a pre-separated .stems ZIP bundle
 * POST /api/upload/audio/separate — upload a flat audio file and trigger auto-separation
 */
@RestController
@RequestMapping("/api/upload")
public class StemUploadController {

    private static final Logger log = LoggerFactory.getLogger(StemUploadController.class);

    private static final Set<String> ACCEPTED_STEM_TYPES =
            Set.of("vocals", "drums", "bass", "instruments", "full_mix");

    @Value("${aws.s3.bucket:starjamz-uploads}")
    private String bucket;

    private final AmazonS3 s3;
    private final TransferManager transferManager;
    private final StemMetadataService stemMetadataService;
    private final DemucsClient demucsClient;

    public StemUploadController(AmazonS3 s3,
                                 TransferManager transferManager,
                                 StemMetadataService stemMetadataService,
                                 DemucsClient demucsClient) {
        this.s3                  = s3;
        this.transferManager     = transferManager;
        this.stemMetadataService = stemMetadataService;
        this.demucsClient        = demucsClient;
    }

    /**
     * Upload a .stems ZIP bundle that contains pre-separated FLAC stem files.
     * Expected ZIP entries: vocals.flac, drums.flac, bass.flac, instruments.flac
     * (at least one is required; full_mix is treated separately).
     */
    @PostMapping("/stems")
    public ResponseEntity<?> uploadStemBundle(
            @RequestParam("file") MultipartFile file,
            @RequestParam("trackId") UUID trackId,
            @RequestParam(value = "uploadedBy", defaultValue = "anonymous") String uploadedBy) {

        if (file.getContentType() == null
                || (!file.getContentType().equals("application/zip")
                    && !file.getContentType().equals("application/x-zip-compressed")
                    && !file.getOriginalFilename().endsWith(".stems"))) {
            return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                    .body("Only .stems (ZIP) bundles are accepted");
        }

        UUID uploadId = UUID.randomUUID();
        Map<String, String> stemS3Keys = new HashMap<>();

        try (ZipInputStream zipIn = new ZipInputStream(file.getInputStream())) {
            ZipEntry entry;
            while ((entry = zipIn.getNextEntry()) != null) {
                if (entry.isDirectory()) continue;

                String entryName = Path.of(entry.getName()).getFileName().toString().toLowerCase();
                // Strip extension to get stem type
                String stemType = entryName.contains(".")
                        ? entryName.substring(0, entryName.lastIndexOf('.'))
                        : entryName;

                if (!ACCEPTED_STEM_TYPES.contains(stemType)) {
                    log.debug("Skipping unknown stem entry: {}", entryName);
                    zipIn.closeEntry();
                    continue;
                }

                String s3Key = String.format("stems/%s/%s/%s.flac",
                        trackId, stemType, uploadId);

                // Buffer the entry to a temp file so we know the content length
                Path tmp = Files.createTempFile("stem_", ".flac");
                try {
                    Files.copy(zipIn, tmp, StandardCopyOption.REPLACE_EXISTING);

                    ObjectMetadata meta = new ObjectMetadata();
                    meta.setContentType("audio/flac");
                    meta.setContentLength(Files.size(tmp));

                    try (InputStream is = Files.newInputStream(tmp)) {
                        Upload upload = transferManager.upload(
                                new PutObjectRequest(bucket, s3Key, is, meta));
                        upload.waitForCompletion();
                    }
                    stemS3Keys.put(stemType, s3Key);
                    log.info("Uploaded stem: track={} type={} s3Key={}", trackId, stemType, s3Key);
                } finally {
                    Files.deleteIfExists(tmp);
                }
                zipIn.closeEntry();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Upload interrupted");
        } catch (IOException e) {
            log.error("Stem bundle upload failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Stem bundle upload failed: " + e.getMessage());
        }

        if (stemS3Keys.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("ZIP contained no recognised stem files (vocals/drums/bass/instruments)");
        }

        stemMetadataService.saveStems(trackId.toString(), stemS3Keys, 0L);

        return ResponseEntity.status(HttpStatus.CREATED).body(
                new StemBundleUploadResponse(trackId, uploadId, stemS3Keys, "ready"));
    }

    /**
     * Upload a flat audio file and trigger automatic Demucs stem separation.
     * The response returns immediately with status="processing"; stem metadata
     * is written to Aerospike asynchronously once the sidecar completes.
     */
    @PostMapping("/audio/separate")
    public ResponseEntity<?> uploadAndSeparate(
            @RequestParam("file") MultipartFile file,
            @RequestParam("trackId") UUID trackId,
            @RequestParam(value = "uploadedBy", defaultValue = "anonymous") String uploadedBy,
            @RequestParam(value = "tier", defaultValue = "2") int tier) {

        UUID uploadId = UUID.randomUUID();
        String safeName = sanitize(file.getOriginalFilename());
        String s3Key = "uploads/audio/" + uploadId + "/" + safeName;

        // Upload original to S3
        try {
            ObjectMetadata meta = new ObjectMetadata();
            meta.setContentType(file.getContentType());
            meta.setContentLength(file.getSize());
            Upload upload = transferManager.upload(
                    new PutObjectRequest(bucket, s3Key, file.getInputStream(), meta));
            upload.waitForCompletion();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Upload interrupted");
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("S3 upload failed: " + e.getMessage());
        }

        // Write full_mix entry immediately so the track is playable right away
        Map<String, String> initialStems = Map.of("full_mix", s3Key);
        stemMetadataService.saveStems(trackId.toString(), initialStems, 0L);

        // Kick off async Demucs separation — response returns before this finishes
        triggerSeparationAsync(s3Key, trackId.toString(), uploadId.toString(), tier);

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(
                new StemBundleUploadResponse(trackId, uploadId,
                        initialStems, "processing"));
    }

    @Async
    protected void triggerSeparationAsync(String s3Key, String trackId,
                                           String uploadId, int tier) {
        log.info("Starting async Demucs separation: trackId={} tier={}", trackId, tier);
        StemSeparationResponse response = demucsClient.requestSeparation(
                s3Key, trackId, uploadId, tier);

        if (response == null || response.stems() == null) {
            log.error("Demucs separation failed for trackId={}", trackId);
            return;
        }
        stemMetadataService.saveStems(trackId, response.stems(), 0L);
        log.info("Stems saved to Aerospike: trackId={} stems={}", trackId, response.stems().keySet());
    }

    private String sanitize(String original) {
        if (original == null || original.isBlank()) return "file";
        return Path.of(original).getFileName().toString()
                   .replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
