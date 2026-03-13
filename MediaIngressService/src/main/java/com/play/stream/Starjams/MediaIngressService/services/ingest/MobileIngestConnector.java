package com.play.stream.Starjams.MediaIngressService.services.ingest;

import com.play.stream.Starjams.MediaIngressService.dto.ConnectorStatusDto;
import com.play.stream.Starjams.MediaIngressService.model.PipelineHealthRecord;
import com.play.stream.Starjams.MediaIngressService.model.StreamPlatform;
import com.play.stream.Starjams.MediaIngressService.services.PipelineHealthService;
import com.play.stream.Starjams.MediaIngressService.services.StreamRouter;
import org.freedesktop.gstreamer.*;
import org.freedesktop.gstreamer.elements.AppSink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles live stream ingestion from mobile clients (iOS and Android).
 *
 * <p>Mobile clients push their stream using GStreamer pipelines:
 *
 * <p><b>iOS (avfvideosrc + osxaudiosrc):</b>
 * <pre>
 *   avfvideosrc
 *     ! video/x-raw,width=1280,height=720,framerate=30/1
 *     ! videoconvert
 *     ! x264enc tune=zerolatency speed-preset=ultrafast
 *     ! video/x-h264,stream-format=byte-stream
 *     ! flvmux name=mux streamable=true
 *   osxaudiosrc
 *     ! audioconvert ! audioresample
 *     ! avenc_aac bitrate=128000 ! aacparse ! mux.
 *   mux. ! rtmpsink location="rtmp://ingest.starjamz.com/live/{streamKey}"
 * </pre>
 *
 * <p><b>Android (ahcsrc camera + openslessrc audio):</b>
 * <pre>
 *   ahcsrc
 *     ! video/x-raw,width=1280,height=720
 *     ! videoconvert
 *     ! x264enc tune=zerolatency speed-preset=ultrafast
 *     ! video/x-h264,stream-format=byte-stream
 *     ! flvmux name=mux streamable=true
 *   openslessrc
 *     ! audioconvert ! audioresample
 *     ! avenc_aac bitrate=128000 ! aacparse ! mux.
 *   mux. ! rtmpsink location="rtmp://ingest.starjamz.com/live/{streamKey}"
 * </pre>
 *
 * <p>Server-side: GStreamer pulls from the RTMP ingest port using {@code rtmpsrc}.
 * The local RTMP relay (e.g., nginx-rtmp or a lightweight Java relay) accepts the
 * push and makes it available at {@code rtmp://localhost:{port}/live/{streamKey}}.
 */
@Service
public class MobileIngestConnector implements PlatformIngestConnector {

    private static final Logger log = LoggerFactory.getLogger(MobileIngestConnector.class);

    private final StreamRouter streamRouter;
    private final PipelineHealthService healthService;

    @Value("${media-ingress.ingest-rtmp-port:1935}")
    private int ingestRtmpPort;

    private volatile boolean enabled = true;

    // streamKey → active pipeline
    private final Map<String, Pipeline> activePipelines = new ConcurrentHashMap<>();

    public MobileIngestConnector(StreamRouter streamRouter, PipelineHealthService healthService) {
        this.streamRouter  = streamRouter;
        this.healthService = healthService;
    }

    @Override
    public StreamPlatform platform() {
        return StreamPlatform.MOBILE_IOS; // handles both iOS and Android
    }

    @Override
    @Async("gstreamerExecutor")
    public void connect(String streamKey, String sourceUrl, String authToken) {
        if (!enabled) {
            log.warn("Mobile ingest connector is disabled — rejecting stream key {}", streamKey);
            return;
        }

        healthService.registerPipeline(streamKey, StreamPlatform.MOBILE_IOS);

        // Pull the RTMP push from the local relay.
        // The mobile client pushed to rtmp://ingest.starjamz.com/live/{streamKey}
        // which is forwarded to rtmp://localhost:{port}/live/{streamKey} by the RTMP relay.
        String rtmpSource = String.format("rtmp://localhost:%d/live/%s", ingestRtmpPort, streamKey);
        String pipelineDesc = String.format(
            "rtmpsrc location=%s ! decodebin name=dec", rtmpSource);

        Pipeline pipeline;
        try {
            pipeline = (Pipeline) Gst.parseLaunch(pipelineDesc);
        } catch (Exception e) {
            log.error("[{}] Failed to create mobile ingest pipeline: {}", streamKey, e.getMessage());
            healthService.recordError(streamKey, e.getMessage());
            return;
        }

        activePipelines.put(streamKey, pipeline);

        // Wire dynamic pads: decodebin emits PAD_ADDED for each decoded stream
        Element decodebin = pipeline.getElementByName("dec");
        if (decodebin != null) {
            decodebin.connect((Element.PAD_ADDED) (element, pad) -> {
                Caps caps = pad.getCurrentCaps();
                if (caps != null) {
                    log.debug("[{}] Mobile ingest pad added: {}", streamKey, caps);
                }
                // Route decoded pads to StreamRouter for transcoding
                streamRouter.routeNewPad(streamKey, pad);
            });
        }

        wireBusListeners(streamKey, pipeline);
        pipeline.setState(State.PLAYING);
        log.info("[{}] Mobile ingest pipeline started from {}", streamKey, rtmpSource);
    }

    @Override
    public void disconnect(String streamKey) {
        Pipeline pipeline = activePipelines.remove(streamKey);
        if (pipeline != null) {
            pipeline.setState(State.NULL);
            pipeline.dispose();
            log.info("[{}] Mobile ingest pipeline stopped", streamKey);
        }
        healthService.deactivatePipeline(streamKey);
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
        log.info("Mobile ingest connector {}", enabled ? "ENABLED" : "DISABLED");
    }

    @Override
    public ConnectorStatusDto getStatus() {
        ConnectorStatusDto dto = new ConnectorStatusDto();
        dto.setPlatform(StreamPlatform.MOBILE_IOS);
        dto.setEnabled(enabled);
        dto.setActiveConnections(activePipelines.size());
        dto.setHealthStatus(enabled ? (activePipelines.isEmpty() ? "ACTIVE" : "ACTIVE") : "DISABLED");
        return dto;
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void wireBusListeners(String streamKey, Pipeline pipeline) {
        Bus bus = pipeline.getBus();

        bus.connect((Bus.ERROR) (source, code, message) -> {
            log.error("[{}] GStreamer ERROR from '{}' (code {}): {}", streamKey, source.getName(), code, message);
            healthService.recordError(streamKey, message);
        });

        bus.connect((Bus.EOS) source -> {
            log.info("[{}] Mobile ingest stream ended (EOS)", streamKey);
            healthService.deactivatePipeline(streamKey);
            activePipelines.remove(streamKey);
        });

        bus.connect((Bus.WARNING) (source, code, message) ->
            log.warn("[{}] GStreamer WARNING from '{}': {}", streamKey, source.getName(), message));
    }
}
