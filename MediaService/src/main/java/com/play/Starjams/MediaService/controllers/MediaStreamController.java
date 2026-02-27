package com.play.Starjams.MediaService.controllers;

import com.play.Starjams.MediaService.models.StreamRequest;
import com.play.Starjams.MediaService.models.StreamSession;
import com.play.Starjams.MediaService.models.StreamType;
import com.play.Starjams.MediaService.services.MediaStreamingService;
import com.play.Starjams.MediaService.services.StreamSessionManager;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.File;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * REST API for creating, monitoring, playing, and stopping media streams.
 *
 * ┌──────────────────────────────────────────────────────────────────────────┐
 * │ Endpoint                              │ Description                      │
 * ├──────────────────────────────────────────────────────────────────────────┤
 * │ POST   /api/streams                   │ Start a new stream session        │
 * │ GET    /api/streams                   │ List all active sessions          │
 * │ GET    /api/streams/{id}              │ Get session details               │
 * │ GET    /api/streams/{id}/play         │ HTTP chunked stream (audio/video) │
 * │ GET    /api/streams/{id}/hls/playlist.m3u8 │ HLS playlist (LIVE_VIDEO)  │
 * │ GET    /api/streams/{id}/hls/{seg}    │ HLS segment (LIVE_VIDEO)         │
 * │ DELETE /api/streams/{id}              │ Stop and remove a session         │
 * └──────────────────────────────────────────────────────────────────────────┘
 *
 * Quick-start examples (replace paths / devices as needed):
 *
 * <pre>
 * # Stream a music file
 * curl -X POST http://localhost:8080/api/streams \
 *      -H 'Content-Type: application/json' \
 *      -d '{"type":"AUDIO_FILE","sourcePath":"/media/music/song.flac"}'
 *
 * # Play the returned session in VLC / browser
 * curl http://localhost:8080/api/streams/{sessionId}/play --output - | mpv -
 *
 * # Stream a video file
 * curl -X POST http://localhost:8080/api/streams \
 *      -d '{"type":"VIDEO_FILE","sourcePath":"/media/video/clip.mp4"}'
 *
 * # Live microphone audio
 * curl -X POST http://localhost:8080/api/streams \
 *      -d '{"type":"LIVE_AUDIO","audioDevice":"default"}'
 *
 * # Live camera + microphone (HLS)
 * curl -X POST http://localhost:8080/api/streams \
 *      -d '{"type":"LIVE_VIDEO","videoDevice":"/dev/video0","audioDevice":"default"}'
 * # → open http://localhost:8080/api/streams/{sessionId}/hls/playlist.m3u8 in VLC
 * </pre>
 */
@RestController
@RequestMapping("/api/streams")
public class MediaStreamController {

    private final MediaStreamingService streamingService;
    private final StreamSessionManager  sessionManager;

    public MediaStreamController(MediaStreamingService streamingService,
                                 StreamSessionManager sessionManager) {
        this.streamingService = streamingService;
        this.sessionManager   = sessionManager;
    }

    // ── Create ────────────────────────────────────────────────────────────────

    /**
     * Creates and immediately starts a new stream session.
     *
     * @return session metadata including the URL to use for playback
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createStream(@RequestBody StreamRequest request) {
        StreamSession session = streamingService.startStream(request);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("sessionId", session.getId());
        body.put("type",      session.getType());
        body.put("status",    session.getStatus());
        body.put("startedAt", session.getStartedAt().toString());

        if (session.getType() == StreamType.LIVE_VIDEO) {
            body.put("hlsPlaylistUrl",
                    "/api/streams/" + session.getId() + "/hls/playlist.m3u8");
        } else {
            body.put("streamUrl",
                    "/api/streams/" + session.getId() + "/play");
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    // ── List / detail ─────────────────────────────────────────────────────────

    @GetMapping
    public Collection<Map<String, Object>> listSessions() {
        return sessionManager.all().stream().map(this::toInfo).toList();
    }

    @GetMapping("/{id}")
    public Map<String, Object> sessionInfo(@PathVariable String id) {
        return sessionManager.get(id)
                .map(this::toInfo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Stream session not found: " + id));
    }

    // ── Play (AppSink-based chunked HTTP response) ────────────────────────────

    /**
     * Streams encoded media bytes directly to the HTTP response as a chunked
     * transfer. Suitable for AUDIO_FILE (audio/mpeg), VIDEO_FILE (video/webm),
     * and LIVE_AUDIO (audio/mpeg).
     *
     * LIVE_VIDEO uses HLS — use the /hls/playlist.m3u8 endpoint instead.
     */
    @GetMapping("/{id}/play")
    public ResponseEntity<StreamingResponseBody> play(@PathVariable String id) {
        StreamSession session = requireSession(id);

        if (session.getType() == StreamType.LIVE_VIDEO) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "LIVE_VIDEO streams use HLS. " +
                    "Open /api/streams/" + id + "/hls/playlist.m3u8 in your player.");
        }

        String contentType = switch (session.getType()) {
            case AUDIO_FILE, LIVE_AUDIO -> "audio/mpeg";
            case VIDEO_FILE             -> "video/webm";
            default                     -> "application/octet-stream";
        };

        StreamingResponseBody body = streamingService.buildResponseBody(session);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
                .header(HttpHeaders.TRANSFER_ENCODING, "chunked")
                .body(body);
    }

    // ── HLS (LIVE_VIDEO) ──────────────────────────────────────────────────────

    /**
     * Serves the HLS playlist (.m3u8) written by the GStreamer hlssink2 element.
     * Open this URL in VLC, Safari, or any HLS-capable player.
     */
    @GetMapping("/{id}/hls/playlist.m3u8")
    public ResponseEntity<Resource> hlsPlaylist(@PathVariable String id) {
        StreamSession session = requireSession(id);
        requireHlsSession(session);

        File playlist = new File(session.getHlsOutputDir() + "/playlist.m3u8");
        if (!playlist.exists()) {
            // Segments may not have been written yet — tell the client to retry
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .header(HttpHeaders.RETRY_AFTER, "2")
                    .build();
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.apple.mpegurl"))
                .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store")
                .body(new FileSystemResource(playlist));
    }

    /**
     * Serves individual HLS transport-stream segments (.ts files).
     * Path traversal attempts are rejected with 400.
     */
    @GetMapping("/{id}/hls/{segment}")
    public ResponseEntity<Resource> hlsSegment(@PathVariable String id,
                                               @PathVariable String segment) {
        StreamSession session = requireSession(id);
        requireHlsSession(session);

        // Guard against path traversal
        if (segment.contains("..") || segment.contains("/") || segment.contains("\\")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid segment name");
        }

        File segFile = new File(session.getHlsOutputDir() + "/" + segment);
        if (!segFile.exists()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("video/mp2t"))
                .header(HttpHeaders.CACHE_CONTROL, "max-age=3600")
                .body(new FileSystemResource(segFile));
    }

    // ── Stop ──────────────────────────────────────────────────────────────────

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> stopStream(@PathVariable String id) {
        streamingService.stopStream(id);
        return ResponseEntity.noContent().build();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private StreamSession requireSession(String id) {
        return sessionManager.get(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Stream session not found: " + id));
    }

    private void requireHlsSession(StreamSession session) {
        if (session.getHlsOutputDir() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "This session does not have an HLS output (type=" + session.getType() + ")");
        }
    }

    private Map<String, Object> toInfo(StreamSession s) {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("sessionId", s.getId());
        info.put("type",      s.getType());
        info.put("status",    s.getStatus());
        info.put("startedAt", s.getStartedAt().toString());

        if (s.getSourcePath() != null) info.put("sourcePath", s.getSourcePath());
        if (s.getDeviceName() != null) info.put("device",     s.getDeviceName());
        if (s.getErrorMessage() != null) info.put("error",    s.getErrorMessage());

        if (s.getType() == StreamType.LIVE_VIDEO) {
            info.put("hlsPlaylistUrl", "/api/streams/" + s.getId() + "/hls/playlist.m3u8");
        } else {
            info.put("streamUrl", "/api/streams/" + s.getId() + "/play");
        }
        return info;
    }
}
