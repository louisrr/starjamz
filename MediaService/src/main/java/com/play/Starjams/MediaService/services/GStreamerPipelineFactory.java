package com.play.Starjams.MediaService.services;

import com.play.Starjams.MediaService.models.StreamSession;
import com.play.Starjams.MediaService.models.StreamStatus;
import org.freedesktop.gstreamer.*;
import org.freedesktop.gstreamer.elements.AppSink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.ByteBuffer;

/**
 * Builds GStreamer {@link Pipeline} objects for each stream type.
 *
 * File-based pipelines (AUDIO_FILE, VIDEO_FILE) are constructed element-by-element
 * so that decodebin's dynamic pads can be wired at runtime via the pad-added signal.
 *
 * Live pipelines (LIVE_AUDIO, LIVE_VIDEO) use parseLaunch because all pads are
 * static and the pipeline description can be expressed as a simple string.
 */
@Component
public class GStreamerPipelineFactory {

    private static final Logger log = LoggerFactory.getLogger(GStreamerPipelineFactory.class);

    @Value("${stream.audio.device:default}")
    private String defaultAudioDevice;

    @Value("${stream.video.device:/dev/video0}")
    private String defaultVideoDevice;

    @Value("${stream.hls.output-dir:/tmp/starjamz-hls}")
    private String hlsBaseDir;

    // ── Public factory methods ────────────────────────────────────────────────

    /**
     * AUDIO_FILE — decodes any audio/video file and encodes the audio track to
     * MP3 for direct HTTP streaming via AppSink.
     *
     * Required plugins: gst-plugins-base (decodebin, audioconvert, audioresample)
     *                   gst-plugins-ugly  (lamemp3enc)
     */
    public Pipeline createAudioFilePipeline(StreamSession session, String filePath) {
        Pipeline pipeline = new Pipeline("audio-file-" + session.getId());

        Element filesrc      = ElementFactory.make("filesrc",       "source");
        Element decodebin    = ElementFactory.make("decodebin",     "decoder");
        Element audioconvert = ElementFactory.make("audioconvert",  "audioconvert");
        Element audioresample= ElementFactory.make("audioresample", "audioresample");
        Element lamemp3enc   = ElementFactory.make("lamemp3enc",    "encoder");
        AppSink sink         = (AppSink) ElementFactory.make("appsink", "sink");

        filesrc.set("location", filePath);
        configureAppSink(sink);

        pipeline.addMany(filesrc, decodebin, audioconvert, audioresample, lamemp3enc, sink);
        filesrc.link(decodebin);
        Element.linkMany(audioconvert, audioresample, lamemp3enc, sink);

        // decodebin has dynamic pads — wire the audio pad when it appears
        decodebin.connect((Element.PAD_ADDED) (element, pad) -> {
            Caps caps = pad.getCurrentCaps();
            if (caps != null && caps.toString().contains("audio")) {
                Pad sinkPad = audioconvert.getStaticPad("sink");
                if (!sinkPad.isLinked()) {
                    try {
                        pad.link(sinkPad);
                    } catch (Exception e) {
                        log.warn("[{}] Audio pad link failed: {}", session.getId(), e.getMessage());
                    }
                }
            }
        });

        setupAppSinkCallback(session, sink);
        wireBusListeners(session, pipeline);
        return pipeline;
    }

    /**
     * VIDEO_FILE — decodes a video file and encodes the video track to VP8/WebM
     * for direct HTTP streaming via AppSink. Audio is muxed in via Vorbis if a
     * compatible audio pad is detected.
     *
     * Required plugins: gst-plugins-base  (decodebin, audioconvert, audioresample, vorbisenc)
     *                   gst-plugins-good  (vp8enc, webmmux)
     */
    public Pipeline createVideoFilePipeline(StreamSession session, String filePath) {
        Pipeline pipeline = new Pipeline("video-file-" + session.getId());

        Element filesrc       = ElementFactory.make("filesrc",        "source");
        Element decodebin     = ElementFactory.make("decodebin",      "decoder");
        Element videoconvert  = ElementFactory.make("videoconvert",   "videoconvert");
        Element vp8enc        = ElementFactory.make("vp8enc",         "videoenc");
        Element videoqueue    = ElementFactory.make("queue",          "videoqueue");
        Element webmmux       = ElementFactory.make("webmmux",        "muxer");
        Element audioconvert  = ElementFactory.make("audioconvert",   "audioconvert");
        Element audioresample = ElementFactory.make("audioresample",  "audioresample");
        Element vorbisenc     = ElementFactory.make("vorbisenc",      "audioenc");
        Element audioqueue    = ElementFactory.make("queue",          "audioqueue");
        AppSink sink          = (AppSink) ElementFactory.make("appsink", "sink");

        filesrc.set("location", filePath);
        vp8enc.set("deadline", 1L);
        configureAppSink(sink);

        pipeline.addMany(filesrc, decodebin,
                videoconvert, vp8enc, videoqueue,
                audioconvert, audioresample, vorbisenc, audioqueue,
                webmmux, sink);

        filesrc.link(decodebin);
        // video branch: videoconvert → vp8enc → queue → webmmux video pad
        Element.linkMany(videoconvert, vp8enc, videoqueue);
        videoqueue.link(webmmux);
        // audio branch: audioconvert → audioresample → vorbisenc → queue → webmmux audio pad
        Element.linkMany(audioconvert, audioresample, vorbisenc, audioqueue);
        audioqueue.link(webmmux);
        // muxer output
        webmmux.link(sink);

        // Wire decodebin's dynamic pads to the correct branch
        decodebin.connect((Element.PAD_ADDED) (element, pad) -> {
            Caps caps = pad.getCurrentCaps();
            if (caps == null) return;
            String capsStr = caps.toString();

            if (capsStr.contains("video")) {
                Pad sinkPad = videoconvert.getStaticPad("sink");
                if (!sinkPad.isLinked()) {
                    try {
                        pad.link(sinkPad);
                    } catch (Exception e) {
                        log.warn("[{}] Video pad link failed: {}", session.getId(), e.getMessage());
                    }
                }
            } else if (capsStr.contains("audio")) {
                Pad sinkPad = audioconvert.getStaticPad("sink");
                if (!sinkPad.isLinked()) {
                    try {
                        pad.link(sinkPad);
                    } catch (Exception e) {
                        log.warn("[{}] Audio pad link failed: {}", session.getId(), e.getMessage());
                    }
                }
            }
        });

        setupAppSinkCallback(session, sink);
        wireBusListeners(session, pipeline);
        return pipeline;
    }

    /**
     * LIVE_AUDIO — captures audio from a PulseAudio device and streams as MP3
     * via AppSink.
     *
     * Required plugins: gst-plugins-good (pulsesrc)
     *                   gst-plugins-base (audioconvert, audioresample)
     *                   gst-plugins-ugly  (lamemp3enc)
     */
    public Pipeline createLiveAudioPipeline(StreamSession session, String device) {
        String dev = resolve(device, defaultAudioDevice);
        String desc = String.format(
            "pulsesrc device=%s ! audioconvert ! audioresample ! lamemp3enc ! appsink name=sink sync=false",
            dev
        );
        return buildParsedAppSinkPipeline(session, desc, "live-audio-");
    }

    /**
     * LIVE_VIDEO — captures video from a V4L2 camera and audio from PulseAudio,
     * encodes to H.264+AAC and writes HLS segments + playlist to disk.
     *
     * Clients play the stream by polling {@code GET /api/streams/{id}/hls/playlist.m3u8}.
     *
     * Required plugins: gst-plugins-good (v4l2src, pulsesrc)
     *                   gst-plugins-bad  (x264enc, hlssink2, avenc_aac, aacparse)
     *                   gst-plugins-base (audioconvert, audioresample)
     */
    public Pipeline createLiveVideoPipeline(StreamSession session, String videoDevice, String audioDevice) {
        String vDev = resolve(videoDevice, defaultVideoDevice);
        String aDev = resolve(audioDevice, defaultAudioDevice);

        String sessionHlsDir = hlsBaseDir + "/" + session.getId();
        new File(sessionHlsDir).mkdirs();
        session.setHlsOutputDir(sessionHlsDir);

        String segLoc      = sessionHlsDir + "/seg%05d.ts";
        String playlistLoc = sessionHlsDir + "/playlist.m3u8";

        // v4l2src and pulsesrc both have static src pads — parseLaunch works fine here
        String desc = String.format(
            "v4l2src device=%s " +
            "  ! video/x-raw,width=1280,height=720,framerate=30/1 " +
            "  ! videoconvert " +
            "  ! x264enc tune=zerolatency speed-preset=ultrafast " +
            "  ! video/x-h264,stream-format=byte-stream " +
            "  ! mpegtsmux name=mux " +
            "pulsesrc device=%s " +
            "  ! audioconvert ! audioresample " +
            "  ! avenc_aac bitrate=128000 ! aacparse ! mux. " +
            "mux. ! hlssink2 location=\"%s\" playlist-location=\"%s\" target-duration=2 max-files=10",
            vDev, aDev, segLoc, playlistLoc
        );

        log.info("[{}] Building LIVE_VIDEO pipeline — video={} audio={}", session.getId(), vDev, aDev);

        Pipeline pipeline;
        try {
            pipeline = (Pipeline) Gst.parseLaunch(desc);
        } catch (Exception e) {
            log.error("[{}] Failed to build LIVE_VIDEO pipeline: {}", session.getId(), e.getMessage());
            session.setStatus(StreamStatus.ERROR);
            session.setErrorMessage(e.getMessage());
            return null;
        }

        wireBusListeners(session, pipeline);
        return pipeline;
    }

    // ── Shared helpers ────────────────────────────────────────────────────────

    /**
     * Builds a pipeline from a parseLaunch description and wires the AppSink
     * callback (used for LIVE_AUDIO where all pads are static).
     */
    private Pipeline buildParsedAppSinkPipeline(StreamSession session, String desc, String namePrefix) {
        log.info("[{}] Parsing pipeline: {}", session.getId(), desc);

        Pipeline pipeline;
        try {
            pipeline = (Pipeline) Gst.parseLaunch(desc);
        } catch (Exception e) {
            log.error("[{}] Failed to parse pipeline: {}", session.getId(), e.getMessage());
            session.setStatus(StreamStatus.ERROR);
            session.setErrorMessage(e.getMessage());
            return null;
        }

        AppSink sink = (AppSink) pipeline.getElementByName("sink");
        configureAppSink(sink);
        setupAppSinkCallback(session, sink);
        wireBusListeners(session, pipeline);
        return pipeline;
    }

    private void configureAppSink(AppSink sink) {
        sink.set("emit-signals", true);
        sink.set("max-buffers", 200);
        sink.set("drop", false);
        sink.set("sync", false);
    }

    /**
     * Connects the AppSink new-sample signal to drain encoded data into the
     * session's blocking queue, which is then drained by the HTTP response thread.
     */
    private void setupAppSinkCallback(StreamSession session, AppSink sink) {
        sink.connect((AppSink.NEW_SAMPLE) appSink -> {
            if (!session.isActive()) return FlowReturn.EOS;

            Sample sample = appSink.pullSample();
            if (sample == null) return FlowReturn.OK;

            try {
                Buffer buf = sample.getBuffer();
                ByteBuffer bb = buf.map(false);
                if (bb != null) {
                    byte[] bytes = new byte[bb.remaining()];
                    bb.get(bytes);
                    buf.unmap();
                    // offer() drops the chunk if the queue is full (back-pressure protection)
                    boolean accepted = session.getDataQueue().offer(bytes);
                    if (!accepted) {
                        log.warn("[{}] DataQueue full — dropping {} bytes", session.getId(), bytes.length);
                    }
                }
            } finally {
                sample.dispose();
            }
            return FlowReturn.OK;
        });
    }

    /** Attaches ERROR, EOS, and WARNING handlers to the pipeline bus. */
    private void wireBusListeners(StreamSession session, Pipeline pipeline) {
        Bus bus = pipeline.getBus();

        bus.connect((Bus.ERROR) (source, code, message) -> {
            log.error("[{}] GStreamer ERROR from '{}' (code {}): {}",
                    session.getId(), source.getName(), code, message);
            session.setStatus(StreamStatus.ERROR);
            session.setErrorMessage(message);
        });

        bus.connect((Bus.EOS) source -> {
            log.info("[{}] End of stream from '{}'", session.getId(), source.getName());
            session.setStatus(StreamStatus.STOPPED);
        });

        bus.connect((Bus.WARNING) (source, code, message) ->
                log.warn("[{}] GStreamer WARNING from '{}': {}", session.getId(), source.getName(), message));
    }

    private String resolve(String requested, String fallback) {
        return (requested != null && !requested.isBlank()) ? requested : fallback;
    }
}
