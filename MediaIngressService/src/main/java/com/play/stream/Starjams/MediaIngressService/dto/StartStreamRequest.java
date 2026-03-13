package com.play.stream.Starjams.MediaIngressService.dto;

import com.play.stream.Starjams.MediaIngressService.model.StreamPlatform;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public class StartStreamRequest {

    @NotNull
    private UUID userId;

    @NotNull
    private StreamPlatform platform;

    /** Required for YOUTUBE, TWITCH, RTMP_GENERIC — the source RTMP URL to pull from. */
    private String platformRtmpUrl;

    /** Optional auth token for external platforms that need it. */
    private String authToken;

    /** Broadcaster display name — used in fan-out feed cards. */
    private String displayName;

    /** Broadcaster avatar URL — used in fan-out feed cards. */
    private String avatarUrl;

    // --- Getters & Setters ---

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public StreamPlatform getPlatform() { return platform; }
    public void setPlatform(StreamPlatform platform) { this.platform = platform; }

    public String getPlatformRtmpUrl() { return platformRtmpUrl; }
    public void setPlatformRtmpUrl(String platformRtmpUrl) { this.platformRtmpUrl = platformRtmpUrl; }

    public String getAuthToken() { return authToken; }
    public void setAuthToken(String authToken) { this.authToken = authToken; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
}
