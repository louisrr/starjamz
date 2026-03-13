package com.play.stream.Starjams.MediaIngressService.services.ingest;

import com.play.stream.Starjams.MediaIngressService.dto.ConnectorStatusDto;
import com.play.stream.Starjams.MediaIngressService.model.PipelineHealthRecord;
import com.play.stream.Starjams.MediaIngressService.model.StreamPlatform;
import com.play.stream.Starjams.MediaIngressService.services.PipelineHealthService;
import com.play.stream.Starjams.MediaIngressService.services.StreamRouter;
import org.freedesktop.gstreamer.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Pulls from any generic RTMP/RTSP source URL provided by the creator.
 *
 * <p>GStreamer pipeline used:
 * <pre>
 *   uridecodebin uri={sourceUrl} name=dec
 * </pre>
 *
 * <p>This connector is also the base for YouTube and Twitch connectors
 * (they each wrap this with their own source URL construction).
 *
 * <p><b>Instagram Live / TikTok Live:</b> These platforms do not expose public
 * RTMP re-stream endpoints. Ingestion is only possible if the creator uses a
 * third-party re-streaming service (e.g., Restream.io) that publishes an RTMP URL.
 * Document this limitation to users who try to connect these platforms.
 */
@Service
public class GenericRtmpConnector implements PlatformIngestConnector {

    private static final Logger log = LoggerFactory.getLogger(GenericRtmpConnector.class);

    protected final StreamRouter streamRouter;
    protected final PipelineHealthService healthService;

    protected volatile boolean enabled = true;

    // streamKey → active pipeline
    protected final Map<String, Pipeline> activePipelines = new ConcurrentHashMap<>();

    public GenericRtmpConnector(StreamRouter streamRouter, PipelineHealthService healthService) {
        this.streamRouter  = streamRouter;
        this.healthService = healthService;
    }

    @Override
    public StreamPlatform platform() {
        return StreamPlatform.RTMP_GENERIC;
    }

    @Override
    @Async("gstreamerExecutor")
    public void connect(String streamKey, String sourceUrl, String authToken) {
        if (!enabled) {
            log.warn("[{}] {} connector is disabled", streamKey, platform());
            return;
        }
        if (sourceUrl == null || sourceUrl.isBlank()) {
            log.error("[{}] No source URL provided for {} connector", streamKey, platform());
            return;
        }

        healthService.registerPipeline(streamKey, platform());

        String pipelineDesc = String.format("uridecodebin uri=%s name=dec", sourceUrl);

        Pipeline pipeline;
        try {
            pipeline = (Pipeline) Gst.parseLaunch(pipelineDesc);
        } catch (Exception e) {
            log.error("[{}] Failed to create {} pipeline: {}", streamKey, platform(), e.getMessage());
            healthService.recordError(streamKey, e.getMessage());
            return;
        }

        activePipelines.put(streamKey, pipeline);

        Element decodebin = pipeline.getElementByName("dec");
        if (decodebin != null) {
            decodebin.connect((Element.PAD_ADDED) (element, pad) -> {
                Caps caps = pad.getCurrentCaps();
                log.debug("[{}] {} pad added: {}", streamKey, platform(), caps);
                streamRouter.routeNewPad(streamKey, pad);
            });
        }

        wireBusListeners(streamKey, pipeline);
        pipeline.setState(State.PLAYING);
        log.info("[{}] {} ingest pipeline started from {}", streamKey, platform(), sourceUrl);
    }

    @Override
    public void disconnect(String streamKey) {
        Pipeline pipeline = activePipelines.remove(streamKey);
        if (pipeline != null) {
            pipeline.setState(State.NULL);
            pipeline.dispose();
        }
        healthService.deactivatePipeline(streamKey);
        log.info("[{}] {} pipeline disconnected", streamKey, platform());
    }

    @Override
    public boolean isConnected(String streamKey) {
        return activePipelines.containsKey(streamKey);
    }

    @Override
    public PipelineHealthRecord getHealth(String streamKey) {
        return healthService.getHealth(streamKey);
    }

    @Override
    public int activeConnectionCount() {
        return activePipelines.size();
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        log.info("{} connector {}", platform(), enabled ? "ENABLED" : "DISABLED");
    }

    @Override
    public ConnectorStatusDto getStatus() {
        ConnectorStatusDto dto = new ConnectorStatusDto();
        dto.setPlatform(platform());
        dto.setEnabled(enabled);
        dto.setActiveConnections(activePipelines.size());
        dto.setHealthStatus(enabled ? "ACTIVE" : "DISABLED");
        return dto;
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    protected void wireBusListeners(String streamKey, Pipeline pipeline) {
        Bus bus = pipeline.getBus();

        bus.connect((Bus.ERROR) (source, code, message) -> {
            log.error("[{}] {} GStreamer ERROR '{}' (code {}): {}",
                streamKey, platform(), source.getName(), code, message);
            healthService.recordError(streamKey, message);
        });

        bus.connect((Bus.EOS) source -> {
            log.info("[{}] {} stream ended (EOS)", streamKey, platform());
            healthService.deactivatePipeline(streamKey);
            activePipelines.remove(streamKey);
        });

        bus.connect((Bus.WARNING) (source, code, message) ->
            log.warn("[{}] {} GStreamer WARNING '{}': {}", streamKey, platform(), source.getName(), message));
    }
}
