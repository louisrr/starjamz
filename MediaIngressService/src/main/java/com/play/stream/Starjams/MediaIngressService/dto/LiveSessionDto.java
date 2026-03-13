package com.play.stream.Starjams.MediaIngressService.dto;

import com.play.stream.Starjams.MediaIngressService.model.StreamPlatform;
import com.play.stream.Starjams.MediaIngressService.model.StreamStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * Live session data transferred from Aerospike to API responses.
 */
public class LiveSessionDto {

    private String streamKey;
    private UUID userId;
    private StreamPlatform platform;
    private StreamStatus status;
    private long viewerCount;
    private Instant startedAt;
    private String hlsManifestUrl;
    private String rtspUrl;
    private String audioOnlyUrl;
    private String thumbnailUrl;

    // --- Getters & Setters ---

    public String getStreamKey() { return streamKey; }
    public void setStreamKey(String streamKey) { this.streamKey = streamKey; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public StreamPlatform getPlatform() { return platform; }
    public void setPlatform(StreamPlatform platform) { this.platform = platform; }

    public StreamStatus getStatus() { return status; }
    public void setStatus(StreamStatus status) { this.status = status; }

    public long getViewerCount() { return viewerCount; }
    public void setViewerCount(long viewerCount) { this.viewerCount = viewerCount; }

    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }

    public String getHlsManifestUrl() { return hlsManifestUrl; }
    public void setHlsManifestUrl(String hlsManifestUrl) { this.hlsManifestUrl = hlsManifestUrl; }

    public String getRtspUrl() { return rtspUrl; }
    public void setRtspUrl(String rtspUrl) { this.rtspUrl = rtspUrl; }

    public String getAudioOnlyUrl() { return audioOnlyUrl; }
    public void setAudioOnlyUrl(String audioOnlyUrl) { this.audioOnlyUrl = audioOnlyUrl; }

    public String getThumbnailUrl() { return thumbnailUrl; }
    public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }
}
