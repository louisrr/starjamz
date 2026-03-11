package com.play.stream.Starjams.FeedService.entity;

import com.play.stream.Starjams.FeedService.model.EventType;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Append-only audit log of all fan-out events — system of record in PostgreSQL.
 * Never used on the hot read path; written asynchronously via Kafka.
 */
@Entity
@Table(
    name = "feed_events_log",
    indexes = {
        @Index(name = "idx_feed_log_actor", columnList = "actor_id"),
        @Index(name = "idx_feed_log_occurred", columnList = "occurred_at")
    }
)
public class FeedEventLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    private EventType eventType;

    @Column(name = "actor_id", nullable = false)
    private UUID actorId;

    @Column(name = "content_id")
    private UUID contentId;

    @Column(name = "fan_out_count")
    private int fanOutCount;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    public FeedEventLog() {}

    public UUID getId() { return id; }
    public UUID getEventId() { return eventId; }
    public void setEventId(UUID eventId) { this.eventId = eventId; }
    public EventType getEventType() { return eventType; }
    public void setEventType(EventType eventType) { this.eventType = eventType; }
    public UUID getActorId() { return actorId; }
    public void setActorId(UUID actorId) { this.actorId = actorId; }
    public UUID getContentId() { return contentId; }
    public void setContentId(UUID contentId) { this.contentId = contentId; }
    public int getFanOutCount() { return fanOutCount; }
    public void setFanOutCount(int fanOutCount) { this.fanOutCount = fanOutCount; }
    public Instant getOccurredAt() { return occurredAt; }
    public void setOccurredAt(Instant occurredAt) { this.occurredAt = occurredAt; }
}
