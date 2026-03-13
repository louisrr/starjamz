package com.play.stream.Starjams.MediaIngressService.services;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.Upload;
import com.play.stream.Starjams.MediaIngressService.model.LiveStream;
import com.play.stream.Starjams.MediaIngressService.model.VodRecording;
import com.play.stream.Starjams.MediaIngressService.repository.LiveStreamRepository;
import com.play.stream.Starjams.MediaIngressService.repository.VodRecordingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Archives completed live streams as MP4 VODs in S3.
 *
 * <p>Archival flow (runs on {@code gstreamerExecutor} thread pool):
 * <ol>
 *   <li>Wait for HLS segment writes to flush (no new files for 5s).</li>
 *   <li>Concatenate TS segments into MP4 using ffmpeg via ProcessBuilder.
 *       GStreamer TS concat is complex; ffmpeg {@code -c copy} is the reliable approach.</li>
 *   <li>Upload MP4 to S3: {@code vod/{userId}/{streamKey}/recording.mp4}</li>
 *   <li>Persist {@link VodRecording} entity to PostgreSQL.</li>
 *   <li>Update {@link LiveStream#setVodS3Key} in PostgreSQL.</li>
 *   <li>Clean up local HLS output directory.</li>
 * </ol>
 */
@Service
public class VodArchivalService {

    private static final Logger log = LoggerFactory.getLogger(VodArchivalService.class);

    private final AmazonS3 s3;
    private final TransferManager transferManager;
    private final LiveStreamRepository liveStreamRepository;
    private final VodRecordingRepository vodRecordingRepository;

    @Value("${media-ingress.hls-output-dir:/tmp/hls}")
    private String hlsOutputDir;

    @Value("${aws.s3.bucket:starjamz-media}")
    private String s3Bucket;

    @Value("${aws.s3.vod-prefix:vod}")
    private String vodPrefix;

    public VodArchivalService(AmazonS3 s3,
                               TransferManager transferManager,
                               LiveStreamRepository liveStreamRepository,
                               VodRecordingRepository vodRecordingRepository) {
        this.s3                    = s3;
        this.transferManager       = transferManager;
        this.liveStreamRepository  = liveStreamRepository;
        this.vodRecordingRepository = vodRecordingRepository;
    }

    @Async("gstreamerExecutor")
    public void archiveStream(String streamKey, UUID userId, LiveStream liveStream) {
        log.info("[{}] Starting VOD archival", streamKey);
        String segmentDir = hlsOutputDir + "/" + streamKey;

        try {
            // 1. Wait for HLS segment writes to flush
            waitForFlush(segmentDir);

            // 2. Collect TS segment files in order
            File dir = new File(segmentDir);
            if (!dir.exists() || !dir.isDirectory()) {
                log.warn("[{}] HLS segment directory not found: {}", streamKey, segmentDir);
                return;
            }

            File[] tsFiles = dir.listFiles((d, name) -> name.endsWith(".ts"));
            if (tsFiles == null || tsFiles.length == 0) {
                log.warn("[{}] No TS segments found for archival", streamKey);
                return;
            }

            Arrays.sort(tsFiles, Comparator.comparing(File::getName));

            // 3. Build ffmpeg concat list file
            File concatList = new File(segmentDir, "concat.txt");
            StringBuilder sb = new StringBuilder();
            for (File ts : tsFiles) {
                sb.append("file '").append(ts.getAbsolutePath()).append("'\n");
            }
            Files.writeString(concatList.toPath(), sb.toString());

            // 4. Run ffmpeg to produce MP4
            File outputMp4 = new File(segmentDir, "recording.mp4");
            int exitCode = runFfmpegConcat(concatList.getAbsolutePath(), outputMp4.getAbsolutePath());

            if (exitCode != 0) {
                log.error("[{}] ffmpeg concat failed with exit code {}", streamKey, exitCode);
                return;
            }

            // 5. Upload to S3: vod/{userId}/{streamKey}/recording.mp4
            String s3Key = String.format("%s/%s/%s/recording.mp4", vodPrefix, userId, streamKey);
            log.info("[{}] Uploading VOD to s3://{}/{}", streamKey, s3Bucket, s3Key);

            Upload upload = transferManager.upload(s3Bucket, s3Key, outputMp4);
            upload.waitForCompletion();

            long fileSizeBytes = outputMp4.length();

            // 6. Estimate duration from segment count × 2s target duration
            int durationSeconds = tsFiles.length * 2;

            // 7. Persist VodRecording to PostgreSQL
            VodRecording vod = new VodRecording();
            vod.setLiveStreamId(liveStream.getId());
            vod.setUserId(userId);
            vod.setS3Key(s3Key);
            vod.setDurationSeconds(durationSeconds);
            vod.setFileSizeBytes(fileSizeBytes);
            vod.setStatus("ACTIVE");
            vodRecordingRepository.save(vod);

            // 8. Update LiveStream.vodS3Key
            liveStream.setVodS3Key(s3Key);
            liveStreamRepository.save(liveStream);

            log.info("[{}] VOD archival complete. s3Key={} duration={}s size={}B",
                streamKey, s3Key, durationSeconds, fileSizeBytes);

        } catch (Exception e) {
            log.error("[{}] VOD archival failed: {}", streamKey, e.getMessage(), e);
        } finally {
            // 9. Clean up local HLS directory
            cleanupLocalSegments(segmentDir);
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Waits until no new TS segments are written for 5 seconds,
     * indicating the HLS mux has flushed all segments.
     */
    private void waitForFlush(String segmentDir) throws InterruptedException {
        File dir = new File(segmentDir);
        long lastModified = 0;
        for (int i = 0; i < 30; i++) { // max 30 × 1s = 30s
            long newest = 0;
            File[] files = dir.listFiles((d, n) -> n.endsWith(".ts"));
            if (files != null) {
                for (File f : files) newest = Math.max(newest, f.lastModified());
            }
            if (newest > 0 && newest == lastModified) {
                Thread.sleep(5000); // wait another 5s to confirm no new writes
                return;
            }
            lastModified = newest;
            Thread.sleep(1000);
        }
    }

    private int runFfmpegConcat(String concatListPath, String outputPath) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(
            "ffmpeg", "-y",
            "-f", "concat", "-safe", "0",
            "-i", concatListPath,
            "-c", "copy",
            outputPath
        );
        pb.redirectErrorStream(true);
        Process process = pb.start();
        boolean finished = process.waitFor(5, TimeUnit.MINUTES);
        if (!finished) {
            process.destroyForcibly();
            return -1;
        }
        return process.exitValue();
    }

    private void cleanupLocalSegments(String segmentDir) {
        try {
            Path dir = Path.of(segmentDir);
            if (Files.exists(dir)) {
                Files.walk(dir)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
            }
        } catch (IOException e) {
            log.warn("Failed to clean up HLS directory {}: {}", segmentDir, e.getMessage());
        }
    }
}
