package com.play.stream.Starjams.FeedService.model;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Core feed event written into each follower's feed bin in Aerospike.
 * All fields are nullable — populate only those relevant to the eventType.
 * TTL is enforced at the Aerospike record level (72 h default).
 */
public class FeedEvent {

    // --- Identity ---
    private UUID   eventId;
    private EventType eventType;

    // --- Actor ---
    private UUID   actorId;
    private String actorDisplayName;
    private String actorAvatarUrl;

    // --- Track payload ---
    private UUID   trackId;
    private String trackTitle;
    private int    trackDuration;          // seconds
    private String coverArtUrl;
    private String audioStreamUrl;
    private List<String> genre;
    private String mood;

    // --- Video payload ---
    private UUID   videoId;
    private String videoTitle;
    private String videoThumbnailUrl;
    private String videoStreamUrl;

    // --- Engagement counters (real-time from Aerospike) ---
    private long playCount;
    private long likeCount;
    private long repostCount;
    private long commentCount;

    // --- Timestamps ---
    private Instant postedAt;
    private Instant expiresAt;

    // --- Viral flags ---
    private boolean isNew;        // posted within 48 h
    private boolean isBuzzing;    // 100+ plays in 6 h

    // --- Gift-to-Unlock ---
    private boolean isLocked;
    private int     giftThreshold;
    private int     giftProgress;

    // --- Repost chain ---
    private UUID   originalActorId;
    private String originalActorDisplayName;
    private String repostCommentText;
    private String repostCommentAudioUrl;
    private List<UUID> repostLineage;       // chain of actor IDs oldest→newest

    // --- Collaborators (up to 3) ---
    private List<CollaboratorRef> collaborators;

    // --- Livestream ---
    private UUID   streamId;
    private boolean isLive;
    private long   viewerCount;
    private String streamThumbnailUrl;
    private Instant streamStartedAt;

    // --- Playlist ---
    private UUID   playlistId;
    private String playlistTitle;
    private List<String> playlistCoverMosaic;

    // --- Feed ranking score (computed, not persisted) ---
    private double rankScore;

    // --- Nested types ---

    public static class CollaboratorRef {
        private UUID   userId;
        private String displayName;
        private String avatarUrl;

        public CollaboratorRef() {}
        public CollaboratorRef(UUID userId, String displayName, String avatarUrl) {
            this.userId = userId;
            this.displayName = displayName;
            this.avatarUrl = avatarUrl;
        }

        public UUID getUserId() { return userId; }
        public void setUserId(UUID userId) { this.userId = userId; }
        public String getDisplayName() { return displayName; }
        public void setDisplayName(String displayName) { this.displayName = displayName; }
        public String getAvatarUrl() { return avatarUrl; }
        public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    }

    // --- Getters & Setters ---

    public UUID getEventId() { return eventId; }
    public void setEventId(UUID eventId) { this.eventId = eventId; }

    public EventType getEventType() { return eventType; }
    public void setEventType(EventType eventType) { this.eventType = eventType; }

    public UUID getActorId() { return actorId; }
    public void setActorId(UUID actorId) { this.actorId = actorId; }

    public String getActorDisplayName() { return actorDisplayName; }
    public void setActorDisplayName(String actorDisplayName) { this.actorDisplayName = actorDisplayName; }

    public String getActorAvatarUrl() { return actorAvatarUrl; }
    public void setActorAvatarUrl(String actorAvatarUrl) { this.actorAvatarUrl = actorAvatarUrl; }

    public UUID getTrackId() { return trackId; }
    public void setTrackId(UUID trackId) { this.trackId = trackId; }

    public String getTrackTitle() { return trackTitle; }
    public void setTrackTitle(String trackTitle) { this.trackTitle = trackTitle; }

    public int getTrackDuration() { return trackDuration; }
    public void setTrackDuration(int trackDuration) { this.trackDuration = trackDuration; }

    public String getCoverArtUrl() { return coverArtUrl; }
    public void setCoverArtUrl(String coverArtUrl) { this.coverArtUrl = coverArtUrl; }

    public String getAudioStreamUrl() { return audioStreamUrl; }
    public void setAudioStreamUrl(String audioStreamUrl) { this.audioStreamUrl = audioStreamUrl; }

    public List<String> getGenre() { return genre; }
    public void setGenre(List<String> genre) { this.genre = genre; }

    public String getMood() { return mood; }
    public void setMood(String mood) { this.mood = mood; }

    public UUID getVideoId() { return videoId; }
    public void setVideoId(UUID videoId) { this.videoId = videoId; }

    public String getVideoTitle() { return videoTitle; }
    public void setVideoTitle(String videoTitle) { this.videoTitle = videoTitle; }

    public String getVideoThumbnailUrl() { return videoThumbnailUrl; }
    public void setVideoThumbnailUrl(String url) { this.videoThumbnailUrl = url; }

    public String getVideoStreamUrl() { return videoStreamUrl; }
    public void setVideoStreamUrl(String url) { this.videoStreamUrl = url; }

    public long getPlayCount() { return playCount; }
    public void setPlayCount(long playCount) { this.playCount = playCount; }

    public long getLikeCount() { return likeCount; }
    public void setLikeCount(long likeCount) { this.likeCount = likeCount; }

    public long getRepostCount() { return repostCount; }
    public void setRepostCount(long repostCount) { this.repostCount = repostCount; }

    public long getCommentCount() { return commentCount; }
    public void setCommentCount(long commentCount) { this.commentCount = commentCount; }

    public Instant getPostedAt() { return postedAt; }
    public void setPostedAt(Instant postedAt) { this.postedAt = postedAt; }

    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }

    public boolean isNew() { return isNew; }
    public void setNew(boolean aNew) { isNew = aNew; }

    public boolean isBuzzing() { return isBuzzing; }
    public void setBuzzing(boolean buzzing) { isBuzzing = buzzing; }

    public boolean isLocked() { return isLocked; }
    public void setLocked(boolean locked) { isLocked = locked; }

    public int getGiftThreshold() { return giftThreshold; }
    public void setGiftThreshold(int giftThreshold) { this.giftThreshold = giftThreshold; }

    public int getGiftProgress() { return giftProgress; }
    public void setGiftProgress(int giftProgress) { this.giftProgress = giftProgress; }

    public UUID getOriginalActorId() { return originalActorId; }
    public void setOriginalActorId(UUID originalActorId) { this.originalActorId = originalActorId; }

    public String getOriginalActorDisplayName() { return originalActorDisplayName; }
    public void setOriginalActorDisplayName(String name) { this.originalActorDisplayName = name; }

    public String getRepostCommentText() { return repostCommentText; }
    public void setRepostCommentText(String text) { this.repostCommentText = text; }

    public String getRepostCommentAudioUrl() { return repostCommentAudioUrl; }
    public void setRepostCommentAudioUrl(String url) { this.repostCommentAudioUrl = url; }

    public List<UUID> getRepostLineage() { return repostLineage; }
    public void setRepostLineage(List<UUID> lineage) { this.repostLineage = lineage; }

    public List<CollaboratorRef> getCollaborators() { return collaborators; }
    public void setCollaborators(List<CollaboratorRef> collaborators) { this.collaborators = collaborators; }

    public UUID getStreamId() { return streamId; }
    public void setStreamId(UUID streamId) { this.streamId = streamId; }

    public boolean isLive() { return isLive; }
    public void setLive(boolean live) { isLive = live; }

    public long getViewerCount() { return viewerCount; }
    public void setViewerCount(long viewerCount) { this.viewerCount = viewerCount; }

    public String getStreamThumbnailUrl() { return streamThumbnailUrl; }
    public void setStreamThumbnailUrl(String url) { this.streamThumbnailUrl = url; }

    public Instant getStreamStartedAt() { return streamStartedAt; }
    public void setStreamStartedAt(Instant streamStartedAt) { this.streamStartedAt = streamStartedAt; }

    public UUID getPlaylistId() { return playlistId; }
    public void setPlaylistId(UUID playlistId) { this.playlistId = playlistId; }

    public String getPlaylistTitle() { return playlistTitle; }
    public void setPlaylistTitle(String playlistTitle) { this.playlistTitle = playlistTitle; }

    public List<String> getPlaylistCoverMosaic() { return playlistCoverMosaic; }
    public void setPlaylistCoverMosaic(List<String> mosaic) { this.playlistCoverMosaic = mosaic; }

    public double getRankScore() { return rankScore; }
    public void setRankScore(double rankScore) { this.rankScore = rankScore; }
}
