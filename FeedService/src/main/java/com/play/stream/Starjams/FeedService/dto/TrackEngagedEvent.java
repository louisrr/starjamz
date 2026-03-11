package com.play.stream.Starjams.FeedService.dto;

import com.play.stream.Starjams.FeedService.model.EventType;

import java.time.Instant;
import java.util.UUID;

/**
 * Kafka message payload published to {@code track.engaged} on play, like, repost,
 * or comment. Consumed by StatsUpdateConsumer to increment Aerospike counters
 * and recompute trending scores.
 */
public class TrackEngagedEvent {

    private UUID      actorId;
    private UUID      trackId;
    private EventType engagementType;   // TRACK_LIKED, TRACK_PLAYED, TRACK_REPOSTED
    private long      actorFollowerCount;
    private Instant   occurredAt;

    // Repost-specific
    private String    repostCommentText;
    private String    repostCommentAudioUrl;

    // Play-specific
    private double    completionPct;   // 0.0–1.0; only fan-out if >0.8

    public TrackEngagedEvent() {}

    public UUID getActorId() { return actorId; }
    public void setActorId(UUID actorId) { this.actorId = actorId; }
    public UUID getTrackId() { return trackId; }
    public void setTrackId(UUID trackId) { this.trackId = trackId; }
    public EventType getEngagementType() { return engagementType; }
    public void setEngagementType(EventType engagementType) { this.engagementType = engagementType; }
    public long getActorFollowerCount() { return actorFollowerCount; }
    public void setActorFollowerCount(long actorFollowerCount) { this.actorFollowerCount = actorFollowerCount; }
    public Instant getOccurredAt() { return occurredAt; }
    public void setOccurredAt(Instant occurredAt) { this.occurredAt = occurredAt; }
    public String getRepostCommentText() { return repostCommentText; }
    public void setRepostCommentText(String text) { this.repostCommentText = text; }
    public String getRepostCommentAudioUrl() { return repostCommentAudioUrl; }
    public void setRepostCommentAudioUrl(String url) { this.repostCommentAudioUrl = url; }
    public double getCompletionPct() { return completionPct; }
    public void setCompletionPct(double completionPct) { this.completionPct = completionPct; }
}
