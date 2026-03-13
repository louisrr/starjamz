package com.play.stream.Starjams.MediaIngressService.dto;

import java.util.UUID;

public class WatchResponse {

    private String streamKey;
    private UUID broadcasterUserId;
    private String hlsManifestUrl;
    private String rtspUrl;
    private String audioOnlyUrl;
    private long viewerCount;
    private String status;

    // --- Getters & Setters ---

    public String getStreamKey() { return streamKey; }
    public void setStreamKey(String streamKey) { this.streamKey = streamKey; }

    public UUID getBroadcasterUserId() { return broadcasterUserId; }
    public void setBroadcasterUserId(UUID broadcasterUserId) { this.broadcasterUserId = broadcasterUserId; }

    public String getHlsManifestUrl() { return hlsManifestUrl; }
    public void setHlsManifestUrl(String hlsManifestUrl) { this.hlsManifestUrl = hlsManifestUrl; }

    public String getRtspUrl() { return rtspUrl; }
    public void setRtspUrl(String rtspUrl) { this.rtspUrl = rtspUrl; }

    public String getAudioOnlyUrl() { return audioOnlyUrl; }
    public void setAudioOnlyUrl(String audioOnlyUrl) { this.audioOnlyUrl = audioOnlyUrl; }

    public long getViewerCount() { return viewerCount; }
    public void setViewerCount(long viewerCount) { this.viewerCount = viewerCount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
