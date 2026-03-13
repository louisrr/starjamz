package com.play.stream.Starjams.LikeService.entity;

import com.play.stream.Starjams.LikeService.model.ContentType;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    name = "likes",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_user_content",
        columnNames = {"user_id", "content_id", "content_type"}
    ),
    indexes = {
        @Index(name = "idx_likes_content", columnList = "content_id, content_type"),
        @Index(name = "idx_likes_user_type_created", columnList = "user_id, content_type, created_at DESC")
    }
)
public class Like {

    @Id
    @Column(name = "like_id", updatable = false, nullable = false)
    private UUID likeId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "content_id", nullable = false)
    private UUID contentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "content_type", nullable = false, length = 16)
    private ContentType contentType;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Like() {}

    public Like(UUID likeId, UUID userId, UUID contentId, ContentType contentType, Instant createdAt) {
        this.likeId = likeId;
        this.userId = userId;
        this.contentId = contentId;
        this.contentType = contentType;
        this.createdAt = createdAt;
    }

    public UUID getLikeId() { return likeId; }
    public UUID getUserId() { return userId; }
    public UUID getContentId() { return contentId; }
    public ContentType getContentType() { return contentType; }
    public Instant getCreatedAt() { return createdAt; }
}
