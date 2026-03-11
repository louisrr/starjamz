package com.play.stream.Starjams.FeedService.model;

import java.util.List;
import java.util.UUID;

/**
 * Native trending-user card injected into the feed at positions 5, 15, 25, ...
 * Personalized by genre overlap with the viewer's listening history.
 */
public class TrendingUserCard {

    private UUID      userId;
    private String    displayName;
    private String    avatarUrl;
    private String    bio;
    private long      followerCount;
    private double    followerGrowthRate;    // % increase in 24h
    private long      weeklyPlayCount;
    private List<TopTrack> topTracks;        // top 3
    private List<String> topGenres;
    private List<MutualFollower> mutualFollowers; // up to 3
    private String    trendingReason;        // "Gaining 2,400 new followers today"
    private boolean   isFollowing;
    private double    rankScore;

    public static class TopTrack {
        private UUID   trackId;
        private String title;
        private long   playCount;

        public TopTrack() {}
        public TopTrack(UUID trackId, String title, long playCount) {
            this.trackId = trackId;
            this.title = title;
            this.playCount = playCount;
        }
        public UUID getTrackId() { return trackId; }
        public void setTrackId(UUID trackId) { this.trackId = trackId; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public long getPlayCount() { return playCount; }
        public void setPlayCount(long playCount) { this.playCount = playCount; }
    }

    public static class MutualFollower {
        private UUID   userId;
        private String displayName;
        public MutualFollower() {}
        public MutualFollower(UUID userId, String displayName) {
            this.userId = userId;
            this.displayName = displayName;
        }
        public UUID getUserId() { return userId; }
        public void setUserId(UUID userId) { this.userId = userId; }
        public String getDisplayName() { return displayName; }
        public void setDisplayName(String displayName) { this.displayName = displayName; }
    }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }
    public long getFollowerCount() { return followerCount; }
    public void setFollowerCount(long followerCount) { this.followerCount = followerCount; }
    public double getFollowerGrowthRate() { return followerGrowthRate; }
    public void setFollowerGrowthRate(double followerGrowthRate) { this.followerGrowthRate = followerGrowthRate; }
    public long getWeeklyPlayCount() { return weeklyPlayCount; }
    public void setWeeklyPlayCount(long weeklyPlayCount) { this.weeklyPlayCount = weeklyPlayCount; }
    public List<TopTrack> getTopTracks() { return topTracks; }
    public void setTopTracks(List<TopTrack> topTracks) { this.topTracks = topTracks; }
    public List<String> getTopGenres() { return topGenres; }
    public void setTopGenres(List<String> topGenres) { this.topGenres = topGenres; }
    public List<MutualFollower> getMutualFollowers() { return mutualFollowers; }
    public void setMutualFollowers(List<MutualFollower> mutualFollowers) { this.mutualFollowers = mutualFollowers; }
    public String getTrendingReason() { return trendingReason; }
    public void setTrendingReason(String trendingReason) { this.trendingReason = trendingReason; }
    public boolean isFollowing() { return isFollowing; }
    public void setFollowing(boolean following) { isFollowing = following; }
    public double getRankScore() { return rankScore; }
    public void setRankScore(double rankScore) { this.rankScore = rankScore; }
}
