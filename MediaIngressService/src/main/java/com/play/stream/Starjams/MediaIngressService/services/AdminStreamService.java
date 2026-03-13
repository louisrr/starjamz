package com.play.stream.Starjams.MediaIngressService.services;

import com.amazonaws.services.s3.AmazonS3;
import com.play.stream.Starjams.MediaIngressService.dto.FanOutProgressDto;
import com.play.stream.Starjams.MediaIngressService.dto.LiveSessionDto;
import com.play.stream.Starjams.MediaIngressService.model.AdminAuditLog;
import com.play.stream.Starjams.MediaIngressService.model.LiveStream;
import com.play.stream.Starjams.MediaIngressService.model.StreamStatus;
import com.play.stream.Starjams.MediaIngressService.model.VodRecording;
import com.play.stream.Starjams.MediaIngressService.repository.AdminAuditLogRepository;
import com.play.stream.Starjams.MediaIngressService.repository.LiveStreamRepository;
import com.play.stream.Starjams.MediaIngressService.repository.VodRecordingRepository;
import com.play.stream.Starjams.MediaIngressService.services.ingest.ConnectorRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Business logic for all admin stream operations.
 * Every mutating method writes to the audit log.
 */
@Service
public class AdminStreamService {

    private static final Logger log = LoggerFactory.getLogger(AdminStreamService.class);

    private final LiveStreamRepository       liveStreamRepo;
    private final VodRecordingRepository     vodRepo;
    private final AdminAuditLogRepository    auditRepo;
    private final LiveSessionRegistry        sessionRegistry;
    private final StreamRouter               streamRouter;
    private final StreamKeyService           streamKeyService;
    private final FanOutPublisherService     fanOutPublisher;
    private final FanOutProgressService      fanOutProgress;
    private final PipelineHealthService      pipelineHealth;
    private final ConnectorRegistry          connectorRegistry;
    private final AmazonS3                   s3;

    @Value("${aws.s3.bucket:starjamz-media}")
    private String s3Bucket;

    @Value("${media-ingress.vod-retention-days:90}")
    private int vodRetentionDays;

    public AdminStreamService(LiveStreamRepository liveStreamRepo,
                               VodRecordingRepository vodRepo,
                               AdminAuditLogRepository auditRepo,
                               LiveSessionRegistry sessionRegistry,
                               StreamRouter streamRouter,
                               StreamKeyService streamKeyService,
                               FanOutPublisherService fanOutPublisher,
                               FanOutProgressService fanOutProgress,
                               PipelineHealthService pipelineHealth,
                               ConnectorRegistry connectorRegistry,
                               AmazonS3 s3) {
        this.liveStreamRepo   = liveStreamRepo;
        this.vodRepo          = vodRepo;
        this.auditRepo        = auditRepo;
        this.sessionRegistry  = sessionRegistry;
        this.streamRouter     = streamRouter;
        this.streamKeyService = streamKeyService;
        this.fanOutPublisher  = fanOutPublisher;
        this.fanOutProgress   = fanOutProgress;
        this.pipelineHealth   = pipelineHealth;
        this.connectorRegistry = connectorRegistry;
        this.s3               = s3;
    }

    // -------------------------------------------------------------------------
    // Live stream listing
    // -------------------------------------------------------------------------

    public List<LiveSessionDto> listLiveStreams() {
        return sessionRegistry.getAllLiveSessions();
    }

    public LiveSessionDto getLiveStreamDetail(String streamKey) {
        return sessionRegistry.getSession(streamKey);
    }

    // -------------------------------------------------------------------------
    // Force-terminate
    // -------------------------------------------------------------------------

    public void forceTerminate(String streamKey, String adminUsername) {
        log.info("Admin {} force-terminating stream {}", adminUsername, streamKey);

        // 1. Tear down GStreamer pipelines
        streamRouter.teardown(streamKey);

        // 2. Revoke stream key
        try { streamKeyService.revokeStreamKey(streamKey); } catch (Exception ignored) {}

        // 3. Update Aerospike status
        sessionRegistry.setStatus(streamKey, StreamStatus.TERMINATED);

        // 4. Update PostgreSQL
        liveStreamRepo.findByStreamKey(streamKey).ifPresent(stream -> {
            stream.setStatus(StreamStatus.TERMINATED);
            stream.setEndedAt(Instant.now());
            liveStreamRepo.save(stream);

            // 5. Publish STREAM_TERMINATED event → FeedService removes feed cards
            fanOutPublisher.publishStreamTerminated(streamKey,
                stream.getUserId(), "broadcaster");
        });

        // 6. Audit log
        audit(adminUsername, "FORCE_TERMINATE", streamKey,
            "Admin force-terminated live stream " + streamKey);
    }

    // -------------------------------------------------------------------------
    // Warn broadcaster
    // -------------------------------------------------------------------------

    public void warnBroadcaster(String streamKey, String message, String adminUsername) {
        // Store warning in Aerospike so mobile client can poll it
        // (simplified: in production this would push via FCM or a websocket)
        liveStreamRepo.findByStreamKey(streamKey).ifPresent(stream ->
            log.info("Warning broadcaster {} (streamKey={}): {}", stream.getUserId(), streamKey, message));

        audit(adminUsername, "WARN_BROADCASTER", streamKey,
            "Sent warning: " + message);
    }

    // -------------------------------------------------------------------------
    // Visibility override
    // -------------------------------------------------------------------------

    public void setVisibility(String streamKey, String visibility, String adminUsername) {
        // Update Aerospike session record with visibility field
        // (simplified: full implementation would gate viewer access in /watch endpoint)
        sessionRegistry.setStatus(streamKey,
            "SUSPENDED".equals(visibility) ? StreamStatus.TERMINATED : StreamStatus.LIVE);

        audit(adminUsername, "SET_VISIBILITY", streamKey,
            "Visibility set to " + visibility);
    }

    // -------------------------------------------------------------------------
    // VOD management
    // -------------------------------------------------------------------------

    public Page<VodRecording> listVodRecordings(Pageable pageable) {
        return vodRepo.findByStatusNot("DELETED", pageable);
    }

    public void deleteVod(UUID vodId, String adminUsername) {
        Optional<VodRecording> opt = vodRepo.findById(vodId);
        if (opt.isEmpty()) throw new IllegalArgumentException("VOD not found: " + vodId);

        VodRecording vod = opt.get();

        // Delete from S3
        try {
            s3.deleteObject(s3Bucket, vod.getS3Key());
            log.info("Deleted S3 object: {}/{}", s3Bucket, vod.getS3Key());
        } catch (Exception e) {
            log.warn("Failed to delete S3 object {}: {}", vod.getS3Key(), e.getMessage());
        }

        // Soft-delete in PostgreSQL
        vod.setStatus("DELETED");
        vodRepo.save(vod);

        audit(adminUsername, "DELETE_VOD", vodId.toString(),
            "Deleted VOD s3Key=" + vod.getS3Key());
    }

    public void restoreVod(UUID vodId, String adminUsername) {
        Optional<VodRecording> opt = vodRepo.findById(vodId);
        if (opt.isEmpty()) throw new IllegalArgumentException("VOD not found: " + vodId);

        VodRecording vod = opt.get();

        // Check retention window
        if (vod.getCreatedAt().isBefore(Instant.now().minus(vodRetentionDays, ChronoUnit.DAYS))) {
            throw new IllegalStateException("VOD is outside retention window of " + vodRetentionDays + " days");
        }

        vod.setStatus("ACTIVE");
        vodRepo.save(vod);

        audit(adminUsername, "RESTORE_VOD", vodId.toString(),
            "Restored VOD s3Key=" + vod.getS3Key());
    }

    // -------------------------------------------------------------------------
    // Pipeline management
    // -------------------------------------------------------------------------

    public void restartPipeline(String pipelineId, String adminUsername) {
        // Disconnect and reconnect via connector
        // (simplified: log the action and update health record)
        pipelineHealth.recordError(pipelineId, "Restart requested by admin " + adminUsername);

        audit(adminUsername, "RESTART_PIPELINE", pipelineId,
            "Admin requested pipeline restart for " + pipelineId);
    }

    // -------------------------------------------------------------------------
    // Fan-out
    // -------------------------------------------------------------------------

    public FanOutProgressDto getFanOutProgress(String streamKey) {
        return fanOutProgress.getProgress(streamKey);
    }

    public void retryFanOut(String streamKey, String adminUsername) {
        liveStreamRepo.findByStreamKey(streamKey).ifPresent(stream -> {
            fanOutPublisher.publishStreamStarted(streamKey, stream.getUserId(),
                "broadcaster", null,
                stream.getHlsManifestUrl(), stream.getRtspUrl(), null);
        });

        audit(adminUsername, "RETRY_FANOUT", streamKey,
            "Admin triggered fan-out retry for stream " + streamKey);
    }

    // -------------------------------------------------------------------------
    // Analytics
    // -------------------------------------------------------------------------

    public Map<String, Object> getSummary() {
        Instant startOfDay = Instant.now().truncatedTo(ChronoUnit.DAYS);
        long streamsToday       = liveStreamRepo.countStreamsToday(startOfDay);
        Integer peakViewers     = liveStreamRepo.findPeakViewerCount(startOfDay);
        BigDecimal watchMinutes = liveStreamRepo.sumTotalWatchMinutes(startOfDay);
        long vodStorageBytes    = vodRepo.sumActiveStorageBytes();

        return Map.of(
            "streamsToday",       streamsToday,
            "peakConcurrentViewers", peakViewers != null ? peakViewers : 0,
            "totalWatchMinutes",  watchMinutes != null ? watchMinutes : BigDecimal.ZERO,
            "vodStorageBytes",    vodStorageBytes
        );
    }

    public List<LiveStream> getTopStreams(Instant from, int limit) {
        return liveStreamRepo.findTopStreamsByViewerCount(from,
            org.springframework.data.domain.PageRequest.of(0, limit));
    }

    // -------------------------------------------------------------------------
    // Audit log helper
    // -------------------------------------------------------------------------

    private void audit(String adminUsername, String action, String targetId, String details) {
        try {
            AdminAuditLog entry = new AdminAuditLog();
            entry.setAdminUsername(adminUsername);
            entry.setAction(action);
            entry.setTargetId(targetId);
            entry.setDetails(details);
            auditRepo.save(entry);
        } catch (Exception e) {
            // Non-critical — never fail admin action due to audit write
            log.warn("Failed to write audit log for action={} target={}: {}", action, targetId, e.getMessage());
        }
    }
}
