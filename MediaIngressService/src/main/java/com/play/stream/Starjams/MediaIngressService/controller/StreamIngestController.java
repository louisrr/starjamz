package com.play.stream.Starjams.MediaIngressService.controller;

import com.play.stream.Starjams.MediaIngressService.dto.*;
import com.play.stream.Starjams.MediaIngressService.model.LiveStream;
import com.play.stream.Starjams.MediaIngressService.model.StreamPlatform;
import com.play.stream.Starjams.MediaIngressService.model.StreamStatus;
import com.play.stream.Starjams.MediaIngressService.repository.LiveStreamRepository;
import com.play.stream.Starjams.MediaIngressService.services.*;
import com.play.stream.Starjams.MediaIngressService.services.ingest.ConnectorRegistry;
import com.play.stream.Starjams.MediaIngressService.services.ingest.PlatformIngestConnector;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for live stream lifecycle management.
 *
 * <p>All endpoints use {@code X-User-Id} header for user identity, following
 * the existing platform pattern (no JWT infrastructure currently exists).
 */
@RestController
@RequestMapping("/api/v1/streams")
public class StreamIngestController {

    private static final Logger log = LoggerFactory.getLogger(StreamIngestController.class);

    private final StreamKeyService      streamKeyService;
    private final LiveSessionRegistry   sessionRegistry;
    private final StreamRouter          streamRouter;
    private final ViewerSessionService  viewerSessionService;
    private final VodArchivalService    vodArchivalService;
    private final FanOutPublisherService fanOutPublisher;
    private final ConnectorRegistry     connectorRegistry;
    private final LiveStreamRepository  liveStreamRepo;

    @Value("${media-ingress.ingest-rtmp-port:1935}")
    private int rtmpPort;

    @Value("${media-ingress.rtsp-server-url:rtsp://localhost:8554}")
    private String rtspServerUrl;

    public StreamIngestController(StreamKeyService streamKeyService,
                                   LiveSessionRegistry sessionRegistry,
                                   StreamRouter streamRouter,
                                   ViewerSessionService viewerSessionService,
                                   VodArchivalService vodArchivalService,
                                   FanOutPublisherService fanOutPublisher,
                                   ConnectorRegistry connectorRegistry,
                                   LiveStreamRepository liveStreamRepo) {
        this.streamKeyService   = streamKeyService;
        this.sessionRegistry    = sessionRegistry;
        this.streamRouter       = streamRouter;
        this.viewerSessionService = viewerSessionService;
        this.vodArchivalService = vodArchivalService;
        this.fanOutPublisher    = fanOutPublisher;
        this.connectorRegistry  = connectorRegistry;
        this.liveStreamRepo     = liveStreamRepo;
    }

    // -------------------------------------------------------------------------
    // POST /api/v1/streams/start — Issue a stream key and begin ingestion
    // -------------------------------------------------------------------------

    @PostMapping("/start")
    public ResponseEntity<?> startStream(@Valid @RequestBody StartStreamRequest request) {
        try {
            UUID userId = request.getUserId();

            // Generate stream key (Aerospike-backed, 6h TTL)
            String streamKey = streamKeyService.generateStreamKey(userId, request.getPlatform());

            // Create live session in Aerospike
            sessionRegistry.createSession(streamKey, userId, request.getPlatform());

            // Set user context in StreamRouter for use when fan-out fires
            streamRouter.setUserContext(streamKey, userId,
                request.getDisplayName(), request.getAvatarUrl());

            // Persist LiveStream entity to PostgreSQL
            LiveStream liveStream = new LiveStream();
            liveStream.setUserId(userId);
            liveStream.setStreamKey(streamKey);
            liveStream.setPlatform(request.getPlatform());
            liveStream.setStatus(StreamStatus.LIVE);
            liveStream.setStartedAt(Instant.now());
            liveStreamRepo.save(liveStream);

            // For external platforms (YouTube, Twitch, generic RTMP), start the pull connector
            StreamPlatform platform = request.getPlatform();
            if (platform != StreamPlatform.MOBILE_IOS && platform != StreamPlatform.MOBILE_ANDROID) {
                if (request.getPlatformRtmpUrl() == null || request.getPlatformRtmpUrl().isBlank()) {
                    return ResponseEntity.badRequest()
                        .body(new ErrorResponse("MISSING_RTMP_URL",
                            "platformRtmpUrl is required for platform: " + platform));
                }
                PlatformIngestConnector connector = connectorRegistry.getConnector(platform);
                connector.connect(streamKey, request.getPlatformRtmpUrl(), request.getAuthToken());
            }
            // For mobile: the client pushes to the RTMP/RTSP ingest endpoint
            // MobileIngestConnector will be triggered when the client push arrives

            // Build ingest URLs for mobile clients
            String rtmpIngestUrl = String.format("rtmp://ingest.starjamz.com/live/%s", streamKey);
            String rtspIngestUrl = String.format("rtsp://ingest.starjamz.com/live/%s", streamKey);
            Instant expiresAt    = Instant.now().plusSeconds(6 * 3600L);

            return ResponseEntity.status(HttpStatus.CREATED)
                .body(new StartStreamResponse(streamKey, rtmpIngestUrl, rtspIngestUrl, expiresAt));

        } catch (Exception e) {
            log.error("Failed to start stream: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(new ErrorResponse("START_FAILED", e.getMessage()));
        }
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/streams/{streamKey}/watch — Get playback URLs
    // -------------------------------------------------------------------------

    @GetMapping("/{streamKey}/watch")
    public ResponseEntity<?> watchStream(@PathVariable String streamKey,
                                          @RequestHeader(value = "X-User-Id", required = false) String userIdHeader) {
        LiveSessionDto session = sessionRegistry.getSession(streamKey);
        if (session == null || session.getStatus() != StreamStatus.LIVE) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("STREAM_NOT_FOUND", "Stream not found or not live: " + streamKey));
        }

        UUID userId = null;
        if (userIdHeader != null && !userIdHeader.isBlank()) {
            try {
                userId = UUID.fromString(userIdHeader);
                viewerSessionService.startViewerSession(streamKey, userId);
            } catch (IllegalArgumentException ignored) {}
        }

        long viewerCount = sessionRegistry.incrementViewerCount(streamKey);

        WatchResponse response = new WatchResponse();
        response.setStreamKey(streamKey);
        response.setBroadcasterUserId(session.getUserId());
        response.setHlsManifestUrl(session.getHlsManifestUrl());
        response.setRtspUrl(session.getRtspUrl());
        response.setAudioOnlyUrl(session.getAudioOnlyUrl());
        response.setViewerCount(viewerCount);
        response.setStatus(session.getStatus().name());

        return ResponseEntity.ok(response);
    }

    // -------------------------------------------------------------------------
    // POST /api/v1/streams/{streamKey}/end — Broadcaster ends the stream
    // -------------------------------------------------------------------------

    @PostMapping("/{streamKey}/end")
    public ResponseEntity<?> endStream(@PathVariable String streamKey,
                                        @RequestHeader(value = "X-User-Id", required = false) String userIdHeader) {
        try {
            // Validate ownership
            if (userIdHeader != null && !userIdHeader.isBlank()) {
                UUID userId = UUID.fromString(userIdHeader);
                if (!streamKeyService.isOwner(streamKey, userId)) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ErrorResponse("FORBIDDEN", "You do not own this stream"));
                }
            }

            LiveSessionDto session = sessionRegistry.getSession(streamKey);
            Instant startedAt = session != null ? session.getStartedAt() : null;
            long durationSeconds = startedAt != null
                ? Instant.now().getEpochSecond() - startedAt.getEpochSecond() : 0;

            // Tear down GStreamer pipelines
            streamRouter.teardown(streamKey);

            // Revoke stream key
            streamKeyService.revokeStreamKey(streamKey);

            // Update Aerospike
            sessionRegistry.endSession(streamKey, durationSeconds);

            // Update PostgreSQL
            liveStreamRepo.findByStreamKey(streamKey).ifPresent(stream -> {
                stream.setStatus(StreamStatus.ENDED);
                stream.setEndedAt(Instant.now());
                liveStreamRepo.save(stream);

                // Publish STREAM_ENDED event → FeedService updates feed cards
                fanOutPublisher.publishStreamEnded(streamKey, stream.getUserId(), null);

                // Trigger async VOD archival
                vodArchivalService.archiveStream(streamKey, stream.getUserId(), stream);
            });

            return ResponseEntity.ok(Map.of("streamKey", streamKey, "status", "ENDED"));

        } catch (Exception e) {
            log.error("Failed to end stream {}: {}", streamKey, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(new ErrorResponse("END_FAILED", e.getMessage()));
        }
    }

    // -------------------------------------------------------------------------
    // POST /api/v1/streams/{streamKey}/heartbeat — Viewer heartbeat
    // -------------------------------------------------------------------------

    @PostMapping("/{streamKey}/heartbeat")
    public ResponseEntity<?> heartbeat(@PathVariable String streamKey,
                                        @RequestHeader(value = "X-User-Id", required = false) String userIdHeader) {
        if (userIdHeader == null || userIdHeader.isBlank()) {
            return ResponseEntity.badRequest()
                .body(new ErrorResponse("MISSING_USER_ID", "X-User-Id header is required"));
        }

        try {
            UUID userId = UUID.fromString(userIdHeader);
            viewerSessionService.heartbeat(streamKey, userId);
            long viewerCount = sessionRegistry.getSession(streamKey) != null
                ? sessionRegistry.getSession(streamKey).getViewerCount() : 0;
            return ResponseEntity.ok(Map.of("viewerCount", viewerCount));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(new ErrorResponse("HEARTBEAT_FAILED", e.getMessage()));
        }
    }

    // -------------------------------------------------------------------------
    // Global exception handler for this controller
    // -------------------------------------------------------------------------

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArg(IllegalArgumentException ex) {
        return ResponseEntity.badRequest()
            .body(new ErrorResponse("BAD_REQUEST", ex.getMessage()));
    }
}
