package com.play.Starjams.MediaService.models;

import org.freedesktop.gstreamer.Pipeline;

import java.time.Instant;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Represents one active (or recently-ended) GStreamer streaming session.
 *
 * For AUDIO_FILE, VIDEO_FILE, and LIVE_AUDIO streams the encoded bytes are
 * produced by the GStreamer AppSink and placed into {@link #dataQueue} so the
 * HTTP response thread can drain them.
 *
 * For LIVE_VIDEO streams the pipeline writes HLS segments to {@link #hlsOutputDir}
 * instead; the data queue is not used.
 */
public class StreamSession {

    private final String id;
    private final StreamType type;
    private final Instant startedAt = Instant.now();

    /** Thread-safe queue of encoded byte chunks produced by the GStreamer AppSink. */
    private final BlockingQueue<byte[]> dataQueue = new LinkedBlockingQueue<>(500);

    private Pipeline pipeline;
    private volatile StreamStatus status = StreamStatus.PENDING;

    /** Absolute path to the source file (AUDIO_FILE / VIDEO_FILE). */
    private String sourcePath;

    /** Capture device identifier (LIVE_AUDIO / LIVE_VIDEO). */
    private String deviceName;

    /** Directory where HLS segments and the playlist are written (LIVE_VIDEO only). */
    private String hlsOutputDir;

    /** Human-readable error message when status == ERROR. */
    private String errorMessage;

    public StreamSession(String id, StreamType type) {
        this.id = id;
        this.type = type;
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    public String getId()                         { return id; }
    public StreamType getType()                   { return type; }
    public Instant getStartedAt()                 { return startedAt; }
    public BlockingQueue<byte[]> getDataQueue()   { return dataQueue; }

    public Pipeline getPipeline()                 { return pipeline; }
    public void setPipeline(Pipeline pipeline)    { this.pipeline = pipeline; }

    public StreamStatus getStatus()               { return status; }
    public void setStatus(StreamStatus status)    { this.status = status; }

    public String getSourcePath()                 { return sourcePath; }
    public void setSourcePath(String sourcePath)  { this.sourcePath = sourcePath; }

    public String getDeviceName()                 { return deviceName; }
    public void setDeviceName(String deviceName)  { this.deviceName = deviceName; }

    public String getHlsOutputDir()               { return hlsOutputDir; }
    public void setHlsOutputDir(String dir)       { this.hlsOutputDir = dir; }

    public String getErrorMessage()               { return errorMessage; }
    public void setErrorMessage(String msg)       { this.errorMessage = msg; }

    /** True while the pipeline is expected to be running. */
    public boolean isActive() {
        return status == StreamStatus.PLAYING || status == StreamStatus.PENDING;
    }
}
