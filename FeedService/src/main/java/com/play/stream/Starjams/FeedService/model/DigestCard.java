package com.play.stream.Starjams.FeedService.model;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Aggregate digest card collapsing multiple high-frequency events (plays, views)
 * into a single feed card. Emitted when 2+ followed users share the same
 * event on the same content within a 4-hour rolling window.
 */
public class DigestCard {

    public enum DigestType { PLAYS, VIEWS }

    private UUID      cardId;
    private DigestType digestType;
    private List<ActorRef> actors;        // up to 5 users
    private UUID      targetTrackId;
    private String    targetTrackTitle;
    private String    targetTrackCoverUrl;
    private UUID      targetVideoId;
    private String    targetVideoTitle;
    private String    targetVideoThumbnailUrl;
    private String    summaryText;        // "Jade, Marcus, and 3 others listened to this"
    private Instant   windowStart;
    private Instant   windowEnd;
    private double    rankScore;

    public static class ActorRef {
        private UUID   userId;
        private String displayName;
        private String avatarUrl;

        public ActorRef() {}
        public ActorRef(UUID userId, String displayName, String avatarUrl) {
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

    public UUID getCardId() { return cardId; }
    public void setCardId(UUID cardId) { this.cardId = cardId; }
    public DigestType getDigestType() { return digestType; }
    public void setDigestType(DigestType digestType) { this.digestType = digestType; }
    public List<ActorRef> getActors() { return actors; }
    public void setActors(List<ActorRef> actors) { this.actors = actors; }
    public UUID getTargetTrackId() { return targetTrackId; }
    public void setTargetTrackId(UUID targetTrackId) { this.targetTrackId = targetTrackId; }
    public String getTargetTrackTitle() { return targetTrackTitle; }
    public void setTargetTrackTitle(String title) { this.targetTrackTitle = title; }
    public String getTargetTrackCoverUrl() { return targetTrackCoverUrl; }
    public void setTargetTrackCoverUrl(String url) { this.targetTrackCoverUrl = url; }
    public UUID getTargetVideoId() { return targetVideoId; }
    public void setTargetVideoId(UUID videoId) { this.targetVideoId = videoId; }
    public String getTargetVideoTitle() { return targetVideoTitle; }
    public void setTargetVideoTitle(String title) { this.targetVideoTitle = title; }
    public String getTargetVideoThumbnailUrl() { return targetVideoThumbnailUrl; }
    public void setTargetVideoThumbnailUrl(String url) { this.targetVideoThumbnailUrl = url; }
    public String getSummaryText() { return summaryText; }
    public void setSummaryText(String summaryText) { this.summaryText = summaryText; }
    public Instant getWindowStart() { return windowStart; }
    public void setWindowStart(Instant windowStart) { this.windowStart = windowStart; }
    public Instant getWindowEnd() { return windowEnd; }
    public void setWindowEnd(Instant windowEnd) { this.windowEnd = windowEnd; }
    public double getRankScore() { return rankScore; }
    public void setRankScore(double rankScore) { this.rankScore = rankScore; }
}
