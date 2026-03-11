package com.play.stream.Starjams.FeedService.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Kafka message payload published to topic {@code track.posted} by UploadService
 * when a user publishes a new track.
 */
public class TrackPostedEvent {

    private UUID   actorId;
    private String actorDisplayName;
    private String actorAvatarUrl;
    private long   actorFollowerCount;

    private UUID   trackId;
    private String trackTitle;
    private int    trackDuration;        // seconds
    private String coverArtUrl;
    private String audioStreamUrl;
    private List<String> genre;
    private String mood;

    private List<UUID> collaboratorIds;  // up to 3 tagged collaborators
    private List<String> collaboratorDisplayNames;

    private boolean isLocked;
    private int     giftThreshold;

    private Instant postedAt;

    // Default constructor required for JSON deserialization
    public TrackPostedEvent() {}

    public UUID getActorId() { return actorId; }
    public void setActorId(UUID actorId) { this.actorId = actorId; }
    public String getActorDisplayName() { return actorDisplayName; }
    public void setActorDisplayName(String name) { this.actorDisplayName = name; }
    public String getActorAvatarUrl() { return actorAvatarUrl; }
    public void setActorAvatarUrl(String url) { this.actorAvatarUrl = url; }
    public long getActorFollowerCount() { return actorFollowerCount; }
    public void setActorFollowerCount(long count) { this.actorFollowerCount = count; }
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
    public List<UUID> getCollaboratorIds() { return collaboratorIds; }
    public void setCollaboratorIds(List<UUID> ids) { this.collaboratorIds = ids; }
    public List<String> getCollaboratorDisplayNames() { return collaboratorDisplayNames; }
    public void setCollaboratorDisplayNames(List<String> names) { this.collaboratorDisplayNames = names; }
    public boolean isLocked() { return isLocked; }
    public void setLocked(boolean locked) { isLocked = locked; }
    public int getGiftThreshold() { return giftThreshold; }
    public void setGiftThreshold(int giftThreshold) { this.giftThreshold = giftThreshold; }
    public Instant getPostedAt() { return postedAt; }
    public void setPostedAt(Instant postedAt) { this.postedAt = postedAt; }
}
