package com.play.stream.Starjams.MediaIngressService.services;

import com.aerospike.client.*;
import com.aerospike.client.policy.ScanPolicy;
import com.aerospike.client.policy.WritePolicy;
import com.play.stream.Starjams.MediaIngressService.dto.LiveSessionDto;
import com.play.stream.Starjams.MediaIngressService.model.StreamPlatform;
import com.play.stream.Starjams.MediaIngressService.model.StreamStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Manages real-time live session state in Aerospike.
 *
 * <p>Aerospike schema:
 * <pre>
 *   namespace: fetio
 *   set:       live_sessions
 *   key:       {streamKey}
 *   bins:      userId, platform, status, startedAt (epochMs),
 *              viewerCount (long), hlsManifestUrl, rtspUrl, audioOnlyUrl, thumbnailUrl
 * </pre>
 *
 * <p>TTL is 0 (no expiry) while LIVE; on stream end, TTL is set to
 * streamDurationSeconds + 86400 for VOD replay availability.
 */
@Service
public class LiveSessionRegistry {

    private static final Logger log = LoggerFactory.getLogger(LiveSessionRegistry.class);
    private static final String NS  = "fetio";
    private static final String SET = "live_sessions";

    private final IAerospikeClient aerospike;

    public LiveSessionRegistry(IAerospikeClient aerospike) {
        this.aerospike = aerospike;
    }

    public void createSession(String streamKey, UUID userId, StreamPlatform platform) {
        Key key = new Key(NS, SET, streamKey);
        WritePolicy wp = new WritePolicy();
        wp.expiration = 0;  // No TTL while LIVE
        wp.sendKey    = true;

        aerospike.put(wp, key,
            new Bin("userId",    userId.toString()),
            new Bin("platform",  platform.name()),
            new Bin("status",    StreamStatus.PENDING.name()),
            new Bin("startedAt", Instant.now().toEpochMilli()),
            new Bin("viewerCount", 0L)
        );
        log.info("Created live session for streamKey={} userId={}", streamKey, userId);
    }

    public void updateUrls(String streamKey, String hlsManifestUrl, String rtspUrl,
                           String audioOnlyUrl, String thumbnailUrl) {
        Key key = new Key(NS, SET, streamKey);
        WritePolicy wp = new WritePolicy();
        wp.sendKey = true;

        aerospike.put(wp, key,
            new Bin("hlsManifestUrl", hlsManifestUrl),
            new Bin("rtspUrl",        rtspUrl),
            new Bin("audioOnlyUrl",   audioOnlyUrl),
            new Bin("thumbnailUrl",   thumbnailUrl),
            new Bin("status",         StreamStatus.LIVE.name())
        );
    }

    /**
     * Atomically increments viewerCount and returns the updated value.
     */
    public long incrementViewerCount(String streamKey) {
        Key key = new Key(NS, SET, streamKey);
        WritePolicy wp = new WritePolicy();
        wp.sendKey = true;

        Record after = aerospike.operate(wp, key,
            Operation.add(new Bin("viewerCount", 1L)),
            Operation.get("viewerCount")
        );

        return after != null ? after.getLong("viewerCount") : 0L;
    }

    public void decrementViewerCount(String streamKey) {
        Key key = new Key(NS, SET, streamKey);
        WritePolicy wp = new WritePolicy();
        wp.sendKey = true;
        aerospike.operate(wp, key, Operation.add(new Bin("viewerCount", -1L)));
    }

    public void setStatus(String streamKey, StreamStatus status) {
        Key key = new Key(NS, SET, streamKey);
        WritePolicy wp = new WritePolicy();
        wp.sendKey = true;
        aerospike.put(wp, key, new Bin("status", status.name()));
    }

    /**
     * Ends a session: sets status to ENDED and applies a TTL of
     * {@code streamDurationSeconds + 24h} so VOD replay cards remain available.
     */
    public void endSession(String streamKey, long streamDurationSeconds) {
        Key key = new Key(NS, SET, streamKey);
        WritePolicy wp = new WritePolicy();
        wp.expiration = (int) Math.min(streamDurationSeconds + 86400L, Integer.MAX_VALUE);
        wp.sendKey    = true;
        aerospike.put(wp, key, new Bin("status", StreamStatus.ENDED.name()));
    }

    public LiveSessionDto getSession(String streamKey) {
        Key key = new Key(NS, SET, streamKey);
        Record rec = aerospike.get(null, key);
        if (rec == null) return null;
        return mapToDto(streamKey, rec);
    }

    /**
     * Returns all sessions with status == LIVE by scanning the set.
     * For production scale this should be backed by a secondary index or a separate live-keys set.
     */
    public List<LiveSessionDto> getAllLiveSessions() {
        List<LiveSessionDto> result = new ArrayList<>();
        ScanPolicy scanPolicy = new ScanPolicy();
        scanPolicy.concurrentNodes = true;

        aerospike.scanAll(scanPolicy, NS, SET, (key, rec) -> {
            if (rec != null && StreamStatus.LIVE.name().equals(rec.getString("status"))) {
                result.add(mapToDto(key.userKey.toString(), rec));
            }
        });
        return result;
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private LiveSessionDto mapToDto(String streamKey, Record rec) {
        LiveSessionDto dto = new LiveSessionDto();
        dto.setStreamKey(streamKey);
        String uid = rec.getString("userId");
        if (uid != null) dto.setUserId(UUID.fromString(uid));
        String platform = rec.getString("platform");
        if (platform != null) dto.setPlatform(StreamPlatform.valueOf(platform));
        String status = rec.getString("status");
        if (status != null) dto.setStatus(StreamStatus.valueOf(status));
        dto.setViewerCount(rec.getLong("viewerCount"));
        long startedAt = rec.getLong("startedAt");
        if (startedAt > 0) dto.setStartedAt(Instant.ofEpochMilli(startedAt));
        dto.setHlsManifestUrl(rec.getString("hlsManifestUrl"));
        dto.setRtspUrl(rec.getString("rtspUrl"));
        dto.setAudioOnlyUrl(rec.getString("audioOnlyUrl"));
        dto.setThumbnailUrl(rec.getString("thumbnailUrl"));
        return dto;
    }
}
