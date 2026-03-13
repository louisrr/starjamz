package com.play.stream.Starjams.MediaIngressService.dto;

import java.time.Instant;

public class StartStreamResponse {

    private String streamKey;

    /**
     * For mobile clients: push your RTMP stream here.
     * {@code rtmp://ingest.starjamz.com/live/{streamKey}}
     */
    private String rtmpIngestUrl;

    /**
     * For mobile clients: alternative RTSP push endpoint.
     * {@code rtsp://ingest.starjamz.com/live/{streamKey}}
     */
    private String rtspIngestUrl;

    private Instant expiresAt;

    public StartStreamResponse() {}

    public StartStreamResponse(String streamKey, String rtmpIngestUrl, String rtspIngestUrl, Instant expiresAt) {
        this.streamKey = streamKey;
        this.rtmpIngestUrl = rtmpIngestUrl;
        this.rtspIngestUrl = rtspIngestUrl;
        this.expiresAt = expiresAt;
    }

    public String getStreamKey() { return streamKey; }
    public void setStreamKey(String streamKey) { this.streamKey = streamKey; }

    public String getRtmpIngestUrl() { return rtmpIngestUrl; }
    public void setRtmpIngestUrl(String rtmpIngestUrl) { this.rtmpIngestUrl = rtmpIngestUrl; }

    public String getRtspIngestUrl() { return rtspIngestUrl; }
    public void setRtspIngestUrl(String rtspIngestUrl) { this.rtspIngestUrl = rtspIngestUrl; }

    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
}
