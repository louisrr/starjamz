package com.play.stream.Starjams.UploadService.controllers;

import com.play.stream.Starjams.UploadService.models.UploadRecord;
import com.play.stream.Starjams.UploadService.services.UploadService;
import com.play.stream.Starjams.UploadService.services.UnsupportedMediaTypeException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/api/upload")
public class UploadController {

    private final UploadService uploadService;

    public UploadController(UploadService uploadService) {
        this.uploadService = uploadService;
    }

    /**
     * Upload an audio file (mp3, wav, flac, aac, ogg, m4a).
     * Multipart field name: "file"
     * Query param: uploadedBy (user ID)
     */
    @PostMapping("/audio")
    public ResponseEntity<?> uploadAudio(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "uploadedBy", defaultValue = "anonymous") String uploadedBy) {
        try {
            UploadRecord record = uploadService.uploadAudio(file, uploadedBy);
            return ResponseEntity.status(HttpStatus.CREATED).body(record);
        } catch (UnsupportedMediaTypeException e) {
            return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(e.getMessage());
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Upload failed: " + e.getMessage());
        }
    }

    /**
     * Upload a video file (mp4, webm, mkv, mov, avi).
     * Multipart field name: "file"
     * Query param: uploadedBy (user ID)
     */
    @PostMapping("/video")
    public ResponseEntity<?> uploadVideo(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "uploadedBy", defaultValue = "anonymous") String uploadedBy) {
        try {
            UploadRecord record = uploadService.uploadVideo(file, uploadedBy);
            return ResponseEntity.status(HttpStatus.CREATED).body(record);
        } catch (UnsupportedMediaTypeException e) {
            return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(e.getMessage());
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Upload failed: " + e.getMessage());
        }
    }

    /**
     * Get metadata for a previously uploaded file by its upload ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getUpload(@PathVariable UUID id) {
        return uploadService.getUpload(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
