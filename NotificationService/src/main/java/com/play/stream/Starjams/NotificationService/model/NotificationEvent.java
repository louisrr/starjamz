package com.play.stream.Starjams.NotificationService.model;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * JSON payload consumed from the {@code notification.event} Kafka topic.
 * Published by FeedService and any other service that needs to trigger a notification.
 */
public class NotificationEvent {

    private UUID             recipientId;      // user to notify
    private UUID             actorId;          // who triggered this (nullable for system events)
    private String           actorDisplayName;
    private String           actorAvatarUrl;
    private NotificationType type;
    private String           title;            // notification title (pre-rendered)
    private String           body;             // notification body text (pre-rendered)
    private Map<String, String> data;          // arbitrary key-value payload for deep-link
    private Instant          occurredAt;
    private String           deduplicationKey; // e.g. "PLAY_STREAK:{userId}:{artistId}"

    public NotificationEvent() {}

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
    public Map<String, String> getData() { return data; }
    public void setData(Map<String, String> data) { this.data = data; }
    public Instant getOccurredAt() { return occurredAt; }
    public void setOccurredAt(Instant occurredAt) { this.occurredAt = occurredAt; }
    public String getDeduplicationKey() { return deduplicationKey; }
    public void setDeduplicationKey(String deduplicationKey) { this.deduplicationKey = deduplicationKey; }
}
