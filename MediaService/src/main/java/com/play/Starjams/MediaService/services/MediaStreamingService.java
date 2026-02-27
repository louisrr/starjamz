package com.play.Starjams.MediaService.services;

import com.play.Starjams.MediaService.models.StreamRequest;
import com.play.Starjams.MediaService.models.StreamSession;
import com.play.Starjams.MediaService.models.StreamStatus;
import org.freedesktop.gstreamer.Pipeline;
import org.freedesktop.gstreamer.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.File;
import java.util.concurrent.TimeUnit;

/**
 * Orchestrates the full lifecycle of a media stream:
 *
 * <ol>
 *   <li>Validates the incoming {@link StreamRequest}.</li>
 *   <li>Creates a {@link StreamSession} and registers it.</li>
 *   <li>Delegates to {@link GStreamerPipelineFactory} to build the GStreamer pipeline.</li>
 *   <li>Starts the pipeline ({@code State.PLAYING}).</li>
 *   <li>For AppSink-based streams: provides a {@link StreamingResponseBody} that drains
 *       the session's data queue into the HTTP response.</li>
 *   <li>For LIVE_VIDEO (HLS): the pipeline writes segments to disk; the controller
 *       serves those files directly.</li>
 * </ol>
 */
@Service
public class MediaStreamingService {

    private static final Logger log = LoggerFactory.getLogger(MediaStreamingService.class);

    private final GStreamerPipelineFactory pipelineFactory;
    private final StreamSessionManager sessionManager;

    public MediaStreamingService(GStreamerPipelineFactory pipelineFactory,
                                 StreamSessionManager sessionManager) {
        this.pipelineFactory = pipelineFactory;
        this.sessionManager  = sessionManager;
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Creates, registers and starts a new stream session.
     *
     * @param request describes what to stream and where to get the source
     * @return the running {@link StreamSession}
     * @throws IllegalArgumentException if required fields are missing or the source file is not found
     * @throws IllegalStateException    if GStreamer fails to build the pipeline
     */
    public StreamSession startStream(StreamRequest request) {
        if (request.getType() == null) {
            throw new IllegalArgumentException("StreamRequest.type must not be null");
        }

        String id = sessionManager.newSessionId();
        StreamSession session = new StreamSession(id, request.getType());

        // Register early so the pipeline factory's AppSink callback can enqueue
        // into this session's data queue from the moment the pipeline starts.
        sessionManager.register(session);

        Pipeline pipeline;
        try {
            pipeline = buildPipeline(request, session);
        } catch (Exception e) {
            session.setStatus(StreamStatus.ERROR);
            session.setErrorMessage(e.getMessage());
            sessionManager.stop(id);   // clean up registration
            throw e;
        }

        if (pipeline == null || session.getStatus() == StreamStatus.ERROR) {
            sessionManager.stop(id);
            throw new IllegalStateException(
                    "GStreamer pipeline could not be created: " + session.getErrorMessage());
        }

        session.setPipeline(pipeline);
        pipeline.setState(State.PLAYING);
        session.setStatus(StreamStatus.PLAYING);
        log.info("[{}] Pipeline started — type={}", id, request.getType());

        return session;
    }

    /**
     * Returns a {@link StreamingResponseBody} that drains encoded media chunks
     * from the session's data queue into the HTTP response until the pipeline
     * ends or the client disconnects.
     *
     * Used by AUDIO_FILE, VIDEO_FILE, and LIVE_AUDIO streams.
     */
    public StreamingResponseBody buildResponseBody(StreamSession session) {
        return outputStream -> {
            log.info("[{}] HTTP client connected — streaming {}", session.getId(), session.getType());
            try {
                while (session.isActive()) {
                    byte[] chunk = session.getDataQueue().poll(200, TimeUnit.MILLISECONDS);
                    if (chunk != null) {
                        outputStream.write(chunk);
                        outputStream.flush();
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                log.info("[{}] HTTP client disconnected: {}", session.getId(), e.getMessage());
            } finally {
                log.info("[{}] HTTP stream ended", session.getId());
                sessionManager.stop(session.getId());
            }
        };
    }

    /** Stops and removes a session by ID (no-op if not found). */
    public void stopStream(String sessionId) {
        sessionManager.stop(sessionId);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private Pipeline buildPipeline(StreamRequest request, StreamSession session) {
        return switch (request.getType()) {
            case AUDIO_FILE -> {
                String path = requirePath(request.getSourcePath());
                session.setSourcePath(path);
                yield pipelineFactory.createAudioFilePipeline(session, path);
            }
            case VIDEO_FILE -> {
                String path = requirePath(request.getSourcePath());
                session.setSourcePath(path);
                yield pipelineFactory.createVideoFilePipeline(session, path);
            }
            case LIVE_AUDIO -> {
                session.setDeviceName(request.getAudioDevice());
                yield pipelineFactory.createLiveAudioPipeline(session, request.getAudioDevice());
            }
            case LIVE_VIDEO -> {
                session.setDeviceName(request.getVideoDevice());
                yield pipelineFactory.createLiveVideoPipeline(
                        session, request.getVideoDevice(), request.getAudioDevice());
            }
        };
    }

    private String requirePath(String path) {
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("sourcePath is required for file-based streams");
        }
        File file = new File(path);
        if (!file.exists() || !file.isFile()) {
            throw new IllegalArgumentException("Media file not found: " + path);
        }
        return path;
    }
}
