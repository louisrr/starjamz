package com.play.stream.Starjams.FeedService.model;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Feed card for active and recently ended livestreams.
 * isLive is flipped to false and replayUrl populated on LIVESTREAM_ENDED.
 * Retracted entirely if stream ended within 60 seconds of starting.
 */
public class LivestreamCard {

    private UUID      streamId;
    private EventType eventType;           // LIVESTREAM_STARTED_AUDIO | VIDEO | ENDED | CLIPPED
    private UUID      actorId;
    private String    actorDisplayName;
    private String    actorAvatarUrl;
    private String    streamTitle;
    private String    streamThumbnailUrl;
    private boolean   isLive;
    private long      viewerCount;
    private long      peakViewerCount;
    private Instant   startedAt;
    private Instant   endedAt;
    private String    replayUrl;
    private List<String> tags;
    private double    rankScore;
    private boolean   pinned;             // true while stream is active

    public UUID getStreamId() { return streamId; }
    public void setStreamId(UUID streamId) { this.streamId = streamId; }
    public EventType getEventType() { return eventType; }
    public void setEventType(EventType eventType) { this.eventType = eventType; }
    public UUID getActorId() { return actorId; }
    public void setActorId(UUID actorId) { this.actorId = actorId; }
    public String getActorDisplayName() { return actorDisplayName; }
    public void setActorDisplayName(String name) { this.actorDisplayName = name; }
    public String getActorAvatarUrl() { return actorAvatarUrl; }
    public void setActorAvatarUrl(String url) { this.actorAvatarUrl = url; }
    public String getStreamTitle() { return streamTitle; }
    public void setStreamTitle(String streamTitle) { this.streamTitle = streamTitle; }
    public String getStreamThumbnailUrl() { return streamThumbnailUrl; }
    public void setStreamThumbnailUrl(String url) { this.streamThumbnailUrl = url; }
    public boolean isLive() { return isLive; }
    public void setLive(boolean live) { isLive = live; }
    public long getViewerCount() { return viewerCount; }
    public void setViewerCount(long viewerCount) { this.viewerCount = viewerCount; }
    public long getPeakViewerCount() { return peakViewerCount; }
    public void setPeakViewerCount(long peakViewerCount) { this.peakViewerCount = peakViewerCount; }
    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }
    public Instant getEndedAt() { return endedAt; }
    public void setEndedAt(Instant endedAt) { this.endedAt = endedAt; }
    public String getReplayUrl() { return replayUrl; }
    public void setReplayUrl(String replayUrl) { this.replayUrl = replayUrl; }
    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }
    public double getRankScore() { return rankScore; }
    public void setRankScore(double rankScore) { this.rankScore = rankScore; }
    public boolean isPinned() { return pinned; }
    public void setPinned(boolean pinned) { this.pinned = pinned; }
}
