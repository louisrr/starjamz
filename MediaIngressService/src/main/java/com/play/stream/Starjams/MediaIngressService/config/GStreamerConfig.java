package com.play.stream.Starjams.MediaIngressService.config;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.freedesktop.gstreamer.Gst;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

/**
 * Initialises and tears down the native GStreamer runtime.
 *
 * <p>Required native packages on the host / container:
 * <pre>
 *   gstreamer1.0-plugins-base  – audioconvert, audioresample, decodebin, vorbisenc
 *   gstreamer1.0-plugins-good  – rtpmanager, rtph264pay, udpsink
 *   gstreamer1.0-plugins-ugly  – lamemp3enc, x264enc
 *   gstreamer1.0-plugins-bad   – hlssink2, avenc_aac, aacparse, rtspclientsink, rtmpsrc
 *   gstreamer1.0-libav         – avdec_* (broad codec support), uridecodebin fallback
 * </pre>
 */
@Configuration
public class GStreamerConfig {

    private static final Logger log = LoggerFactory.getLogger(GStreamerConfig.class);

    @PostConstruct
    public void init() {
        log.info("Initialising GStreamer…");
        Gst.init("Starjamz-MediaIngressService");
        log.info("GStreamer ready — version {}", Gst.getVersionString());
    }

    @PreDestroy
    public void cleanup() {
        log.info("Shutting down GStreamer…");
        Gst.deinit();
    }
}
