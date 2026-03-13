package com.play.stream.Starjams.MediaIngressService.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.UUID;

/**
 * Kafka event payload published to the {@code livestream.event} topic.
 *
 * <p>Field names mirror those of {@code FeedEvent} in FeedService so that
 * {@code FeedFanoutConsumer.onLivestreamEvent} can deserialise this directly
 * into a {@code FeedEvent} without any changes.
 *
 * <p>FeedFanoutConsumer already handles this topic and calls
 * {@code fanOutLivestream(event, followers)}, which fans out to all followers
 * with batching for large broadcasters (>10,000 followers).
 */
public class LivestreamKafkaEvent {

    private UUID eventId;
    private String eventType;       // "LIVESTREAM_STARTED_VIDEO" | "LIVESTREAM_ENDED"
    private UUID actorId;           // broadcaster userId
    private String actorDisplayName;
    private String actorAvatarUrl;

    // Livestream-specific fields (mapped to FeedEvent.stream* bins)
    private UUID streamId;          // = UUID derived from streamKey
    @JsonProperty("isLive")
    private boolean live;
    private String hlsManifestUrl;
    private String rtspUrl;
    private String streamThumbnailUrl;
    private long viewerCount;
    private Instant streamStartedAt;
    private Instant postedAt;       // = streamStartedAt (for feed ranking recency)

    // --- Getters & Setters ---

    public UUID getEventId() { return eventId; }
    public void setEventId(UUID eventId) { this.eventId = eventId; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public UUID getActorId() { return actorId; }
    public void setActorId(UUID actorId) { this.actorId = actorId; }

    public String getActorDisplayName() { return actorDisplayName; }
    public void setActorDisplayName(String actorDisplayName) { this.actorDisplayName = actorDisplayName; }

    public String getActorAvatarUrl() { return actorAvatarUrl; }
    public void setActorAvatarUrl(String actorAvatarUrl) { this.actorAvatarUrl = actorAvatarUrl; }

    public UUID getStreamId() { return streamId; }
    public void setStreamId(UUID streamId) { this.streamId = streamId; }

    public boolean isLive() { return live; }
    public void setLive(boolean live) { this.live = live; }

    public String getHlsManifestUrl() { return hlsManifestUrl; }
    public void setHlsManifestUrl(String hlsManifestUrl) { this.hlsManifestUrl = hlsManifestUrl; }

    public String getRtspUrl() { return rtspUrl; }
    public void setRtspUrl(String rtspUrl) { this.rtspUrl = rtspUrl; }

    public String getStreamThumbnailUrl() { return streamThumbnailUrl; }
    public void setStreamThumbnailUrl(String streamThumbnailUrl) { this.streamThumbnailUrl = streamThumbnailUrl; }

    public long getViewerCount() { return viewerCount; }
    public void setViewerCount(long viewerCount) { this.viewerCount = viewerCount; }

    public Instant getStreamStartedAt() { return streamStartedAt; }
    public void setStreamStartedAt(Instant streamStartedAt) { this.streamStartedAt = streamStartedAt; }

    public Instant getPostedAt() { return postedAt; }
    public void setPostedAt(Instant postedAt) { this.postedAt = postedAt; }
}
