package com.play.stream.Starjams.MediaIngressService.services.ingest;

import com.play.stream.Starjams.MediaIngressService.dto.ConnectorStatusDto;
import com.play.stream.Starjams.MediaIngressService.model.PipelineHealthRecord;
import com.play.stream.Starjams.MediaIngressService.model.StreamPlatform;

/**
 * Contract for all platform ingest connectors.
 *
 * <p>Each implementation is responsible for:
 * <ol>
 *   <li>Opening a GStreamer pipeline to ingest audio/video from a source.
 *   <li>Running all pipeline operations on the {@code gstreamerExecutor} thread pool.
 *   <li>Handing the decoded pipeline to {@link StreamRouter} once PLAYING.
 *   <li>Reporting health metrics to {@link com.play.stream.Starjams.MediaIngressService.services.PipelineHealthService}.
 * </ol>
 */
public interface PlatformIngestConnector {

    /** The platform this connector handles. */
    StreamPlatform platform();

    /**
     * Connects to the stream source and starts ingesting.
     *
     * @param streamKey  The session key — used as pipeline ID.
     * @param sourceUrl  Source RTMP/RTSP URL (null for mobile push; the server listens instead).
     * @param authToken  Optional auth token for platforms that require it.
     */
    void connect(String streamKey, String sourceUrl, String authToken);

    /** Gracefully tears down the pipeline for this stream key. */
    void disconnect(String streamKey);

    boolean isConnected(String streamKey);

    PipelineHealthRecord getHealth(String streamKey);

    int activeConnectionCount();

    boolean isEnabled();

    void setEnabled(boolean enabled);

    ConnectorStatusDto getStatus();
}
