package com.play.stream.Starjams.MediaIngressService.model;

import java.time.Instant;

/**
 * In-memory snapshot of a GStreamer ingest pipeline's health.
 * Maintained by {@link com.play.stream.Starjams.MediaIngressService.services.PipelineHealthService}.
 */
public class PipelineHealthRecord {

    private final String pipelineId;       // = streamKey
    private final StreamPlatform platform;
    private final Instant startedAt;

    private volatile long bitrateKbps;
    private volatile long droppedFrames;
    private volatile long errorCount;
    private volatile String lastError;
    private volatile boolean active;

    public PipelineHealthRecord(String pipelineId, StreamPlatform platform) {
        this.pipelineId = pipelineId;
        this.platform   = platform;
        this.startedAt  = Instant.now();
        this.active     = true;
    }

    // --- Getters ---

    public String getPipelineId()     { return pipelineId; }
    public StreamPlatform getPlatform() { return platform; }
    public Instant getStartedAt()     { return startedAt; }
    public long getBitrateKbps()      { return bitrateKbps; }
    public long getDroppedFrames()    { return droppedFrames; }
    public long getErrorCount()       { return errorCount; }
    public String getLastError()      { return lastError; }
    public boolean isActive()         { return active; }

    // --- Setters ---

    public void setBitrateKbps(long bitrateKbps)   { this.bitrateKbps = bitrateKbps; }
    public void setDroppedFrames(long droppedFrames) { this.droppedFrames = droppedFrames; }
    public void setActive(boolean active)            { this.active = active; }

    public void recordError(String error) {
        this.errorCount++;
        this.lastError = error;
    }

    public void incrementDroppedFrames() {
        this.droppedFrames++;
    }
}
