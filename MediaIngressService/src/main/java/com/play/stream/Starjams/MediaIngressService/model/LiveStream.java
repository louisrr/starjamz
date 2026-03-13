package com.play.stream.Starjams.MediaIngressService.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Durable record of a live stream session.
 * Real-time state (viewerCount, status) lives in Aerospike; this is the system-of-record.
 */
@Entity
@Table(name = "live_streams", indexes = {
    @Index(name = "idx_live_streams_user_id", columnList = "user_id"),
    @Index(name = "idx_live_streams_status",  columnList = "status"),
    @Index(name = "idx_live_streams_started_at", columnList = "started_at")
})
public class LiveStream {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "stream_key", unique = true, nullable = false, length = 64)
    private String streamKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "platform", nullable = false, length = 32)
    private StreamPlatform platform;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private StreamStatus status;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    @Column(name = "peak_viewer_count")
    private int peakViewerCount;

    @Column(name = "total_watch_minutes", precision = 10, scale = 2)
    private BigDecimal totalWatchMinutes = BigDecimal.ZERO;

    @Column(name = "hls_manifest_url", columnDefinition = "TEXT")
    private String hlsManifestUrl;

    @Column(name = "rtsp_url", columnDefinition = "TEXT")
    private String rtspUrl;

    @Column(name = "vod_s3_key", columnDefinition = "TEXT")
    private String vodS3Key;

    @Column(name = "created_at")
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (id == null) id = UUID.randomUUID();
        if (createdAt == null) createdAt = Instant.now();
        if (startedAt == null) startedAt = Instant.now();
    }

    // --- Getters & Setters ---

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public String getStreamKey() { return streamKey; }
    public void setStreamKey(String streamKey) { this.streamKey = streamKey; }

    public StreamPlatform getPlatform() { return platform; }
    public void setPlatform(StreamPlatform platform) { this.platform = platform; }

    public StreamStatus getStatus() { return status; }
    public void setStatus(StreamStatus status) { this.status = status; }

    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }

    public Instant getEndedAt() { return endedAt; }
    public void setEndedAt(Instant endedAt) { this.endedAt = endedAt; }

    public int getPeakViewerCount() { return peakViewerCount; }
    public void setPeakViewerCount(int peakViewerCount) { this.peakViewerCount = peakViewerCount; }

    public BigDecimal getTotalWatchMinutes() { return totalWatchMinutes; }
    public void setTotalWatchMinutes(BigDecimal totalWatchMinutes) { this.totalWatchMinutes = totalWatchMinutes; }

    public String getHlsManifestUrl() { return hlsManifestUrl; }
    public void setHlsManifestUrl(String hlsManifestUrl) { this.hlsManifestUrl = hlsManifestUrl; }

    public String getRtspUrl() { return rtspUrl; }
    public void setRtspUrl(String rtspUrl) { this.rtspUrl = rtspUrl; }

    public String getVodS3Key() { return vodS3Key; }
    public void setVodS3Key(String vodS3Key) { this.vodS3Key = vodS3Key; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
