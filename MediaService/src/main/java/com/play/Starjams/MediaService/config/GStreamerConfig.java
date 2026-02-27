package com.play.Starjams.MediaService.config;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.freedesktop.gstreamer.Gst;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

/**
 * Initialises and tears down the native GStreamer runtime.
 *
 * GStreamer must be installed on the host (e.g. libgstreamer1.0-dev) together
 * with the plugin packages your pipelines rely on:
 *
 *   gstreamer1.0-plugins-base   – audioconvert, audioresample, decodebin, vorbisenc
 *   gstreamer1.0-plugins-good   – pulsesrc, v4l2src, vp8enc, webmmux
 *   gstreamer1.0-plugins-ugly   – lamemp3enc
 *   gstreamer1.0-plugins-bad    – hlssink2, x264enc, avenc_aac, aacparse
 *   gstreamer1.0-libav          – avdec_* family (broad codec support)
 */
@Configuration
public class GStreamerConfig {

    private static final Logger log = LoggerFactory.getLogger(GStreamerConfig.class);

    @PostConstruct
    public void init() {
        log.info("Initialising GStreamer…");
        Gst.init("Starjamz-MediaService");
        log.info("GStreamer ready — version {}", Gst.getVersionString());
    }

    @PreDestroy
    public void cleanup() {
        log.info("Shutting down GStreamer…");
        Gst.deinit();
    }
}
