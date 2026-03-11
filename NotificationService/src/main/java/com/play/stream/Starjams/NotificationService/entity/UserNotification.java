package com.play.stream.Starjams.NotificationService.entity;

import com.play.stream.Starjams.NotificationService.model.NotificationType;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Durable notification record in PostgreSQL.
 * Used for the in-app notification inbox and read-state tracking.
 */
@Entity
@Table(
    name = "user_notifications",
    indexes = {
        @Index(name = "idx_notif_recipient", columnList = "recipient_id, created_at DESC"),
        @Index(name = "idx_notif_unread",    columnList = "recipient_id, is_read")
    }
)
public class UserNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "recipient_id", nullable = false)
    private UUID recipientId;

    @Column(name = "actor_id")
    private UUID actorId;

    @Column(name = "actor_display_name")
    private String actorDisplayName;

    @Column(name = "actor_avatar_url")
    private String actorAvatarUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false)
    private NotificationType type;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "body", columnDefinition = "TEXT")
    private String body;

    @Column(name = "deep_link_url")
    private String deepLinkUrl;

    @Column(name = "is_read", nullable = false)
    private boolean isRead = false;

    @Column(name = "is_pushed", nullable = false)
    private boolean isPushed = false;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public UserNotification() {}

    public UUID getId() { return id; }
    public UUID getRecipientId() { return recipientId; }
    public void setRecipientId(UUID recipientId) { this.recipientId = recipientId; }
    public UUID getActorId() { return actorId; }
    public void setActorId(UUID actorId) { this.actorId = actorId; }
    public String getActorDisplayName() { return actorDisplayName; }
    public void setActorDisplayName(String actorDisplayName) { this.actorDisplayName = actorDisplayName; }
    public String getActorAvatarUrl() { return actorAvatarUrl; }
    public void setActorAvatarUrl(String actorAvatarUrl) { this.actorAvatarUrl = actorAvatarUrl; }
    public NotificationType getType() { return type; }
    public void setType(NotificationType type) { this.type = type; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    public String getDeepLinkUrl() { return deepLinkUrl; }
    public void setDeepLinkUrl(String deepLinkUrl) { this.deepLinkUrl = deepLinkUrl; }
    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }
    public boolean isPushed() { return isPushed; }
    public void setPushed(boolean pushed) { isPushed = pushed; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
