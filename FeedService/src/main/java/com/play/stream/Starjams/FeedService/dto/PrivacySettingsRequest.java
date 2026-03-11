package com.play.stream.Starjams.FeedService.dto;

/**
 * Request body for {@code POST /api/v1/users/{userId}/privacy}.
 * Controls which activity types this user shares into followers' feeds.
 */
public class PrivacySettingsRequest {

    private boolean shareLikes   = true;
    private boolean shareViews   = true;
    private boolean sharePlays   = true;
    private boolean shareFollows = true;

    public boolean isShareLikes() { return shareLikes; }
    public void setShareLikes(boolean shareLikes) { this.shareLikes = shareLikes; }
    public boolean isShareViews() { return shareViews; }
    public void setShareViews(boolean shareViews) { this.shareViews = shareViews; }
    public boolean isSharePlays() { return sharePlays; }
    public void setSharePlays(boolean sharePlays) { this.sharePlays = sharePlays; }
    public boolean isShareFollows() { return shareFollows; }
    public void setShareFollows(boolean shareFollows) { this.shareFollows = shareFollows; }
}
