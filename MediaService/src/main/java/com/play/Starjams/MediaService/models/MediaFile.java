package com.play.Starjams.MediaService.models;

import org.springframework.data.cassandra.core.mapping.*;
import java.time.Instant;
import java.util.UUID;

@Table("media_files")
public class MediaFile {

    @PrimaryKey
    private UUID id;

    @Column("filename")
    private String filename;         // no nullable constraint — enforce in service layer

    @Column("content_type")
    private String contentType;

    @Column("storage_path")
    private String storagePath;

    @Column("file_size")
    private Long fileSize;

    @Column("uploaded_by")
    private String uploadedBy;

    @Column("uploaded_at")
    private Instant uploadedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public String getStoragePath() { return storagePath; }
    public void setStoragePath(String storagePath) { this.storagePath = storagePath; }

    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }

    public String getUploadedBy() { return uploadedBy; }
    public void setUploadedBy(String uploadedBy) { this.uploadedBy = uploadedBy; }

    public Instant getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(Instant uploadedAt) { this.uploadedAt = uploadedAt; }
}
