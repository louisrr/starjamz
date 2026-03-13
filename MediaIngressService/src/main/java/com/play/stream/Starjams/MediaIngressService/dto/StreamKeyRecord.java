package com.play.stream.Starjams.MediaIngressService.dto;

import com.play.stream.Starjams.MediaIngressService.model.StreamPlatform;

import java.time.Instant;
import java.util.UUID;

/** In-memory representation of a stream key fetched from Aerospike. */
public class StreamKeyRecord {

    private String streamKey;
    private UUID userId;
    private StreamPlatform platform;
    private Instant createdAt;
    private Instant expiresAt;

    // --- Getters & Setters ---

    public String getStreamKey() { return streamKey; }
    public void setStreamKey(String streamKey) { this.streamKey = streamKey; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public StreamPlatform getPlatform() { return platform; }
    public void setPlatform(StreamPlatform platform) { this.platform = platform; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
}
