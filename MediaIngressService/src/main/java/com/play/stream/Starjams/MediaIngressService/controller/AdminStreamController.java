package com.play.stream.Starjams.MediaIngressService.controller;

import com.play.stream.Starjams.MediaIngressService.dto.*;
import com.play.stream.Starjams.MediaIngressService.model.PipelineHealthRecord;
import com.play.stream.Starjams.MediaIngressService.model.StreamPlatform;
import com.play.stream.Starjams.MediaIngressService.model.VodRecording;
import com.play.stream.Starjams.MediaIngressService.services.AdminStreamService;
import com.play.stream.Starjams.MediaIngressService.services.FanOutProgressService;
import com.play.stream.Starjams.MediaIngressService.services.PipelineHealthService;
import com.play.stream.Starjams.MediaIngressService.services.ingest.ConnectorRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Admin REST controller for live stream management.
 *
 * <p>All endpoints require the ADMIN role (enforced by {@code SecurityConfig}).
 * Every mutating action is written to the audit log via {@link AdminStreamService}.
 */
@RestController
@RequestMapping("/api/v1/admin/streams")
public class AdminStreamController {

    private static final Logger log = LoggerFactory.getLogger(AdminStreamController.class);

    private final AdminStreamService     adminStreamService;
    private final FanOutProgressService  fanOutProgressService;
    private final PipelineHealthService  pipelineHealthService;
    private final ConnectorRegistry      connectorRegistry;

    public AdminStreamController(AdminStreamService adminStreamService,
                                  FanOutProgressService fanOutProgressService,
                                  PipelineHealthService pipelineHealthService,
                                  ConnectorRegistry connectorRegistry) {
        this.adminStreamService   = adminStreamService;
        this.fanOutProgressService = fanOutProgressService;
        this.pipelineHealthService = pipelineHealthService;
        this.connectorRegistry    = connectorRegistry;
    }

    // =========================================================================
    // Live Stream Controls
    // =========================================================================

    /** GET /api/v1/admin/streams/live — paginated list of currently live streams */
    @GetMapping("/live")
    public ResponseEntity<List<LiveSessionDto>> listLiveStreams() {
        return ResponseEntity.ok(adminStreamService.listLiveStreams());
    }

    /** GET /api/v1/admin/streams/live/{streamKey} — full detail for one live session */
    @GetMapping("/live/{streamKey}")
    public ResponseEntity<?> getLiveStreamDetail(@PathVariable String streamKey) {
        LiveSessionDto session = adminStreamService.getLiveStreamDetail(streamKey);
        if (session == null) {
            return ResponseEntity.notFound().build();
        }
        PipelineHealthRecord health = pipelineHealthService.getHealth(streamKey);
        FanOutProgressDto    fanOut = fanOutProgressService.getProgress(streamKey);

        return ResponseEntity.ok(Map.of(
            "session", session,
            "pipelineHealth", health != null ? health : Map.of(),
            "fanOutProgress",  fanOut
        ));
    }

    /** DELETE /api/v1/admin/streams/live/{streamKey} — force-terminate a stream */
    @DeleteMapping("/live/{streamKey}")
    public ResponseEntity<?> forceTerminate(@PathVariable String streamKey,
                                             @AuthenticationPrincipal UserDetails user) {
        adminStreamService.forceTerminate(streamKey, user.getUsername());
        return ResponseEntity.ok(Map.of("streamKey", streamKey, "status", "TERMINATED"));
    }

    /** POST /api/v1/admin/streams/live/{streamKey}/warn — send warning to broadcaster */
    @PostMapping("/live/{streamKey}/warn")
    public ResponseEntity<?> warnBroadcaster(@PathVariable String streamKey,
                                              @RequestBody Map<String, String> body,
                                              @AuthenticationPrincipal UserDetails user) {
        String message = body.getOrDefault("message", "");
        if (message.isBlank()) {
            return ResponseEntity.badRequest()
                .body(new ErrorResponse("MISSING_MESSAGE", "message field is required"));
        }
        adminStreamService.warnBroadcaster(streamKey, message, user.getUsername());
        return ResponseEntity.ok(Map.of("streamKey", streamKey, "warned", true));
    }

    /** PUT /api/v1/admin/streams/live/{streamKey}/visibility */
    @PutMapping("/live/{streamKey}/visibility")
    public ResponseEntity<?> setVisibility(@PathVariable String streamKey,
                                            @RequestBody Map<String, String> body,
                                            @AuthenticationPrincipal UserDetails user) {
        String visibility = body.getOrDefault("visibility", "");
        if (!List.of("PUBLIC", "FOLLOWERS_ONLY", "SUSPENDED").contains(visibility)) {
            return ResponseEntity.badRequest()
                .body(new ErrorResponse("INVALID_VISIBILITY",
                    "visibility must be PUBLIC, FOLLOWERS_ONLY, or SUSPENDED"));
        }
        adminStreamService.setVisibility(streamKey, visibility, user.getUsername());
        return ResponseEntity.ok(Map.of("streamKey", streamKey, "visibility", visibility));
    }

    // =========================================================================
    // VOD Controls
    // =========================================================================

    /** GET /api/v1/admin/streams/vod — paginated VOD recording list */
    @GetMapping("/vod")
    public ResponseEntity<Page<VodRecording>> listVods(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(adminStreamService.listVodRecordings(
            PageRequest.of(page, size, Sort.by("createdAt").descending())));
    }

    /** DELETE /api/v1/admin/streams/vod/{vodId} — delete VOD */
    @DeleteMapping("/vod/{vodId}")
    public ResponseEntity<?> deleteVod(@PathVariable UUID vodId,
                                        @AuthenticationPrincipal UserDetails user) {
        adminStreamService.deleteVod(vodId, user.getUsername());
        return ResponseEntity.ok(Map.of("vodId", vodId, "status", "DELETED"));
    }

    /** POST /api/v1/admin/streams/vod/{vodId}/restore — restore soft-deleted VOD */
    @PostMapping("/vod/{vodId}/restore")
    public ResponseEntity<?> restoreVod(@PathVariable UUID vodId,
                                         @AuthenticationPrincipal UserDetails user) {
        adminStreamService.restoreVod(vodId, user.getUsername());
        return ResponseEntity.ok(Map.of("vodId", vodId, "status", "ACTIVE"));
    }

    // =========================================================================
    // Ingest Health
    // =========================================================================

    /** GET /api/v1/admin/streams/ingest/health */
    @GetMapping("/ingest/health")
    public ResponseEntity<List<PipelineHealthRecord>> ingestHealth() {
        return ResponseEntity.ok(pipelineHealthService.getAllActive());
    }

    /** GET /api/v1/admin/streams/ingest/queue */
    @GetMapping("/ingest/queue")
    public ResponseEntity<List<ConnectorStatusDto>> ingestQueue() {
        return ResponseEntity.ok(connectorRegistry.getQueueStatus());
    }

    /** POST /api/v1/admin/streams/ingest/pipeline/{pipelineId}/restart */
    @PostMapping("/ingest/pipeline/{pipelineId}/restart")
    public ResponseEntity<?> restartPipeline(@PathVariable String pipelineId,
                                              @AuthenticationPrincipal UserDetails user) {
        adminStreamService.restartPipeline(pipelineId, user.getUsername());
        return ResponseEntity.ok(Map.of("pipelineId", pipelineId, "action", "RESTART_REQUESTED"));
    }

    // =========================================================================
    // Fan-Out Monitoring
    // =========================================================================

    /** GET /api/v1/admin/streams/{streamKey}/fanout */
    @GetMapping("/{streamKey}/fanout")
    public ResponseEntity<FanOutProgressDto> getFanOutProgress(@PathVariable String streamKey) {
        return ResponseEntity.ok(fanOutProgressService.getProgress(streamKey));
    }

    /** POST /api/v1/admin/streams/{streamKey}/fanout/retry */
    @PostMapping("/{streamKey}/fanout/retry")
    public ResponseEntity<?> retryFanOut(@PathVariable String streamKey,
                                          @AuthenticationPrincipal UserDetails user) {
        adminStreamService.retryFanOut(streamKey, user.getUsername());
        return ResponseEntity.ok(Map.of("streamKey", streamKey, "fanOutRetried", true));
    }

    // =========================================================================
    // Platform Connector Management
    // =========================================================================

    /** GET /api/v1/admin/streams/connectors */
    @GetMapping("/connectors")
    public ResponseEntity<List<ConnectorStatusDto>> listConnectors() {
        return ResponseEntity.ok(connectorRegistry.getConnectorStatuses());
    }

    /** PUT /api/v1/admin/streams/connectors/{platform}/disable */
    @PutMapping("/connectors/{platform}/disable")
    public ResponseEntity<?> disableConnector(@PathVariable String platform,
                                               @AuthenticationPrincipal UserDetails user) {
        StreamPlatform p = StreamPlatform.valueOf(platform.toUpperCase());
        connectorRegistry.setEnabled(p, false);
        return ResponseEntity.ok(Map.of("platform", platform, "enabled", false));
    }

    /** PUT /api/v1/admin/streams/connectors/{platform}/enable */
    @PutMapping("/connectors/{platform}/enable")
    public ResponseEntity<?> enableConnector(@PathVariable String platform,
                                              @AuthenticationPrincipal UserDetails user) {
        StreamPlatform p = StreamPlatform.valueOf(platform.toUpperCase());
        connectorRegistry.setEnabled(p, true);
        return ResponseEntity.ok(Map.of("platform", platform, "enabled", true));
    }

    // =========================================================================
    // Analytics
    // =========================================================================

    /** GET /api/v1/admin/streams/analytics/summary */
    @GetMapping("/analytics/summary")
    public ResponseEntity<Map<String, Object>> analyticsSummary() {
        return ResponseEntity.ok(adminStreamService.getSummary());
    }

    /**
     * GET /api/v1/admin/streams/analytics/top
     *
     * @param hours Number of hours to look back (default 24)
     * @param limit Max results (default 10)
     */
    @GetMapping("/analytics/top")
    public ResponseEntity<?> topStreams(
            @RequestParam(defaultValue = "24") int hours,
            @RequestParam(defaultValue = "10") int limit) {
        Instant from = Instant.now().minus(hours, ChronoUnit.HOURS);
        return ResponseEntity.ok(adminStreamService.getTopStreams(from, limit));
    }

    // =========================================================================
    // Exception handling
    // =========================================================================

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArg(IllegalArgumentException ex) {
        return ResponseEntity.badRequest()
            .body(new ErrorResponse("BAD_REQUEST", ex.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(IllegalStateException ex) {
        return ResponseEntity.status(422)
            .body(new ErrorResponse("UNPROCESSABLE", ex.getMessage()));
    }
}
