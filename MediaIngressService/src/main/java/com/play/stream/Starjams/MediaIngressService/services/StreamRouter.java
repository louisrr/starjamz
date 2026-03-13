package com.play.stream.Starjams.MediaIngressService.services;

import org.freedesktop.gstreamer.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Core GStreamer transcode and fan-out engine.
 *
 * <p>For each live stream, StreamRouter creates three output branches:
 * <ol>
 *   <li><b>HLS</b> — mpegtsmux → hlssink2 (2-second segments), written to local disk,
 *       served via CDN. Playlist: {@code {hlsOutputDir}/{streamKey}/playlist.m3u8}</li>
 *   <li><b>RTSP re-stream</b> — x264enc → rtph264pay → rtspclientsink, pushing to
 *       the embedded RTSP server.</li>
 *   <li><b>Audio-only MP3</b> — audioconvert → lamemp3enc → AppSink (exposed as
 *       a streaming HTTP endpoint for low-bandwidth listeners).</li>
 * </ol>
 *
 * <p>All pipeline operations run on the {@code gstreamerExecutor} thread pool.
 *
 * <p><b>Dynamic pad routing:</b> When an ingest connector calls {@link #routeNewPad},
 * StreamRouter detects whether the pad carries video or audio and links it to the
 * appropriate branch elements.
 */
@Service
public class StreamRouter {

    private static final Logger log = LoggerFactory.getLogger(StreamRouter.class);

    private final PipelineHealthService healthService;
    private final LiveSessionRegistry sessionRegistry;
    private final FanOutPublisherService fanOutPublisher;

    @Value("${media-ingress.hls-output-dir:/tmp/hls}")
    private String hlsOutputDir;

    @Value("${media-ingress.rtsp-server-url:rtsp://localhost:8554}")
    private String rtspServerUrl;

    @Value("${media-ingress.cdn-base-url:https://cdn.starjamz.com}")
    private String cdnBaseUrl;

    // streamKey → routing session
    private final Map<String, RoutingSession> sessions = new ConcurrentHashMap<>();

    public StreamRouter(PipelineHealthService healthService,
                        LiveSessionRegistry sessionRegistry,
                        FanOutPublisherService fanOutPublisher) {
        this.healthService   = healthService;
        this.sessionRegistry = sessionRegistry;
        this.fanOutPublisher  = fanOutPublisher;
    }

    /**
     * Called by ingest connectors when a new decoded pad becomes available.
     * Routes video pads to HLS + RTSP branches, audio pads to all branches.
     * Runs on the {@code gstreamerExecutor} thread pool.
     */
    @Async("gstreamerExecutor")
    public void routeNewPad(String streamKey, Pad pad) {
        RoutingSession session = sessions.computeIfAbsent(streamKey, k -> new RoutingSession(k));

        Caps caps = pad.getCurrentCaps();
        if (caps == null) return;
        String capsStr = caps.toString();

        if (capsStr.contains("video")) {
            log.info("[{}] Routing video pad to HLS + RTSP branches", streamKey);
            session.linkVideoPad(pad);
            if (session.isReadyToStart()) {
                startOutputPipelines(streamKey, session);
            }
        } else if (capsStr.contains("audio")) {
            log.info("[{}] Routing audio pad to all output branches", streamKey);
            session.linkAudioPad(pad);
            if (session.isReadyToStart()) {
                startOutputPipelines(streamKey, session);
            }
        }
    }

    /**
     * Starts all three output pipelines once both video and audio pads are available.
     */
    private void startOutputPipelines(String streamKey, RoutingSession session) {
        if (!session.startOnce.compareAndSet(false, true)) {
            return; // Already started
        }

        String outputDir = hlsOutputDir + "/" + streamKey;
        new File(outputDir).mkdirs();

        // Branch A — HLS output
        // Uses mpegtsmux + hlssink2 (target-duration=2, max-files=0 keeps all segments for VOD)
        String hlsDesc = String.format(
            "videoconvert name=hlsvc" +
            " ! x264enc tune=zerolatency speed-preset=ultrafast key-int-max=60" +
            " ! video/x-h264,stream-format=byte-stream" +
            " ! mpegtsmux name=hlsmux" +
            " audioconvert name=hlsac ! audioresample ! avenc_aac bitrate=128000 ! aacparse ! hlsmux." +
            " hlsmux. ! hlssink2 location=\"%s/segment%%05d.ts\" playlist-location=\"%s/playlist.m3u8\"" +
            "          target-duration=2 max-files=0",
            outputDir, outputDir);

        // Branch B — RTSP re-stream
        String rtspDesc = String.format(
            "videoconvert name=rtspvc" +
            " ! x264enc tune=zerolatency speed-preset=ultrafast" +
            " ! rtph264pay config-interval=1 pt=96" +
            " ! rtspclientsink location=%s/%s protocols=tcp",
            rtspServerUrl, streamKey);

        // Branch C — Audio-only MP3 AppSink
        String audioDesc =
            "audioconvert name=aoac ! audioresample ! lamemp3enc bitrate=128" +
            " ! appsink name=audioOnlySink sync=false emit-signals=true";

        try {
            Pipeline hlsPipeline  = (Pipeline) Gst.parseLaunch(hlsDesc);
            Pipeline rtspPipeline = (Pipeline) Gst.parseLaunch(rtspDesc);
            Pipeline audioPipeline = (Pipeline) Gst.parseLaunch(audioDesc);

            session.hlsPipeline   = hlsPipeline;
            session.rtspPipeline  = rtspPipeline;
            session.audioPipeline = audioPipeline;

            wireBusListeners(streamKey, hlsPipeline,   "HLS");
            wireBusListeners(streamKey, rtspPipeline,  "RTSP");
            wireBusListeners(streamKey, audioPipeline, "AUDIO");

            hlsPipeline.setState(State.PLAYING);
            rtspPipeline.setState(State.PLAYING);
            audioPipeline.setState(State.PLAYING);

            // Compute public-facing URLs
            String hlsManifestUrl = cdnBaseUrl + "/hls/" + streamKey + "/playlist.m3u8";
            String rtspUrl        = rtspServerUrl + "/" + streamKey;
            String audioOnlyUrl   = cdnBaseUrl + "/audio/" + streamKey + "/stream.mp3";

            sessionRegistry.updateUrls(streamKey, hlsManifestUrl, rtspUrl, audioOnlyUrl, null);

            // Publish STREAM_STARTED to Kafka → FeedFanoutConsumer fans out to all followers
            fanOutPublisher.publishStreamStarted(streamKey, session.getUserId(),
                session.getDisplayName(), session.getAvatarUrl(),
                hlsManifestUrl, rtspUrl, null);

            log.info("[{}] Output pipelines started. HLS={} RTSP={}", streamKey, hlsManifestUrl, rtspUrl);

        } catch (Exception e) {
            log.error("[{}] Failed to start output pipelines: {}", streamKey, e.getMessage(), e);
            healthService.recordError(streamKey, e.getMessage());
        }
    }

    /**
     * Gracefully tears down all GStreamer pipelines for a stream.
     */
    public void teardown(String streamKey) {
        RoutingSession session = sessions.remove(streamKey);
        if (session == null) return;

        for (Pipeline p : new Pipeline[]{session.hlsPipeline, session.rtspPipeline, session.audioPipeline}) {
            if (p != null) {
                try {
                    p.setState(State.NULL);
                    p.dispose();
                } catch (Exception e) {
                    log.warn("[{}] Error stopping pipeline: {}", streamKey, e.getMessage());
                }
            }
        }

        healthService.removePipeline(streamKey);
        log.info("[{}] All output pipelines torn down", streamKey);
    }

    public void setUserContext(String streamKey, java.util.UUID userId, String displayName, String avatarUrl) {
        RoutingSession session = sessions.computeIfAbsent(streamKey, k -> new RoutingSession(k));
        session.setUserId(userId);
        session.setDisplayName(displayName);
        session.setAvatarUrl(avatarUrl);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void wireBusListeners(String streamKey, Pipeline pipeline, String branchName) {
        Bus bus = pipeline.getBus();

        bus.connect((Bus.ERROR) (source, code, message) -> {
            log.error("[{}] {} pipeline ERROR '{}' (code {}): {}",
                streamKey, branchName, source.getName(), code, message);
            healthService.recordError(streamKey, branchName + ": " + message);
        });

        bus.connect((Bus.EOS) source ->
            log.info("[{}] {} pipeline EOS", streamKey, branchName));

        bus.connect((Bus.WARNING) (source, code, message) ->
            log.warn("[{}] {} pipeline WARNING '{}': {}", streamKey, branchName, source.getName(), message));
    }

    // -------------------------------------------------------------------------
    // Inner class: per-stream routing state
    // -------------------------------------------------------------------------

    private static class RoutingSession {
        final String streamKey;
        final AtomicBoolean startOnce = new AtomicBoolean(false);

        volatile Pad videoPad;
        volatile Pad audioPad;
        volatile Pipeline hlsPipeline;
        volatile Pipeline rtspPipeline;
        volatile Pipeline audioPipeline;

        private volatile java.util.UUID userId;
        private volatile String displayName;
        private volatile String avatarUrl;

        RoutingSession(String streamKey) {
            this.streamKey = streamKey;
        }

        void linkVideoPad(Pad pad) { this.videoPad = pad; }
        void linkAudioPad(Pad pad) { this.audioPad = pad; }

        boolean isReadyToStart() {
            return videoPad != null && audioPad != null;
        }

        java.util.UUID getUserId()    { return userId; }
        String getDisplayName()        { return displayName; }
        String getAvatarUrl()          { return avatarUrl; }

        void setUserId(java.util.UUID userId)       { this.userId = userId; }
        void setDisplayName(String displayName)     { this.displayName = displayName; }
        void setAvatarUrl(String avatarUrl)         { this.avatarUrl = avatarUrl; }
    }
}
