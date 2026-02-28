package com.play.stream.Starjams.UploadService.services;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.play.stream.Starjams.UploadService.models.UploadMediaType;
import com.play.stream.Starjams.UploadService.models.UploadRecord;
import com.play.stream.Starjams.UploadService.repositories.UploadRecordRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class UploadServiceImpl implements UploadService {

    private static final Set<String> ACCEPTED_AUDIO_TYPES = Set.of(
            "audio/mpeg", "audio/wav", "audio/flac", "audio/aac",
            "audio/ogg", "audio/x-m4a", "audio/mp4"
    );

    private static final Set<String> ACCEPTED_VIDEO_TYPES = Set.of(
            "video/mp4", "video/webm", "video/x-matroska",
            "video/quicktime", "video/x-msvideo", "video/mpeg"
    );

    @Value("${aws.s3.bucket:starjamz-uploads}")
    private String bucket;

    private final AmazonS3 s3;
    private final UploadRecordRepository repository;
    private final UploadEventPublisher eventPublisher;

    public UploadServiceImpl(AmazonS3 s3, UploadRecordRepository repository, UploadEventPublisher eventPublisher) {
        this.s3 = s3;
        this.repository = repository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public UploadRecord uploadAudio(MultipartFile file, String uploadedBy) throws IOException {
        validateContentType(file.getContentType(), ACCEPTED_AUDIO_TYPES, "audio");
        UploadRecord record = store(file, uploadedBy, UploadMediaType.AUDIO, "uploads/audio");
        eventPublisher.publishAudioUpload(record);
        return record;
    }

    @Override
    public UploadRecord uploadVideo(MultipartFile file, String uploadedBy) throws IOException {
        validateContentType(file.getContentType(), ACCEPTED_VIDEO_TYPES, "video");
        UploadRecord record = store(file, uploadedBy, UploadMediaType.VIDEO, "uploads/video");
        eventPublisher.publishVideoUpload(record);
        return record;
    }

    @Override
    public Optional<UploadRecord> getUpload(UUID id) {
        return repository.findById(id);
    }

    private UploadRecord store(MultipartFile file, String uploadedBy, UploadMediaType mediaType, String prefix) throws IOException {
        String uploadId = UUID.randomUUID().toString();
        String s3Key = prefix + "/" + uploadId + "/" + file.getOriginalFilename();

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType(file.getContentType());
        metadata.setContentLength(file.getSize());

        s3.putObject(new PutObjectRequest(bucket, s3Key, file.getInputStream(), metadata));
        String s3Url = s3.getUrl(bucket, s3Key).toString();

        UploadRecord record = new UploadRecord();
        record.setOriginalFileName(file.getOriginalFilename());
        record.setContentType(file.getContentType());
        record.setFileSize(file.getSize());
        record.setS3Bucket(bucket);
        record.setS3Key(s3Key);
        record.setS3Url(s3Url);
        record.setMediaType(mediaType);
        record.setUploadedBy(uploadedBy);
        record.setUploadedAt(LocalDateTime.now());

        return repository.save(record);
    }

    private void validateContentType(String contentType, Set<String> accepted, String label) {
        if (contentType == null || !accepted.contains(contentType)) {
            throw new UnsupportedMediaTypeException(
                    "Unsupported " + label + " content type: " + contentType +
                    ". Accepted: " + accepted
            );
        }
    }
}
