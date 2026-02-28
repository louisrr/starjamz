package com.play.stream.Starjams.UploadService.services;

import com.play.stream.Starjams.UploadService.models.UploadRecord;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

public interface UploadService {
    UploadRecord uploadAudio(MultipartFile file, String uploadedBy) throws IOException;
    UploadRecord uploadVideo(MultipartFile file, String uploadedBy) throws IOException;
    Optional<UploadRecord> getUpload(UUID id);
}
