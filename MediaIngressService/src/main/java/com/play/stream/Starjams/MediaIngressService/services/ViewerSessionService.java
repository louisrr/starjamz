package com.play.stream.Starjams.MediaIngressService.services;

import com.aerospike.client.*;
import com.aerospike.client.policy.WritePolicy;
import com.play.stream.Starjams.MediaIngressService.model.LiveStream;
import com.play.stream.Starjams.MediaIngressService.repository.LiveStreamRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Manages viewer session lifecycle in Aerospike.
 *
 * <p>Aerospike schema:
 * <pre>
 *   namespace: fetio
 *   set:       viewer_sessions
 *   key:       {streamKey}:{userId}
 *   TTL:       24 hours
 *   bins:      joinedAt (epochMs), lastHeartbeatAt (epochMs), watchDurationSeconds (long)
 * </pre>
 */
@Service
public class ViewerSessionService {

    private static final Logger log = LoggerFactory.getLogger(ViewerSessionService.class);
    private static final String NS  = "fetio";
    private static final String SET = "viewer_sessions";

    private final IAerospikeClient aerospike;
    private final LiveSessionRegistry liveSessionRegistry;
    private final LiveStreamRepository liveStreamRepository;

    public ViewerSessionService(IAerospikeClient aerospike,
                                  LiveSessionRegistry liveSessionRegistry,
                                  LiveStreamRepository liveStreamRepository) {
        this.aerospike             = aerospike;
        this.liveSessionRegistry   = liveSessionRegistry;
        this.liveStreamRepository  = liveStreamRepository;
    }

    public void startViewerSession(String streamKey, UUID userId) {
        Key key = new Key(NS, SET, streamKey + ":" + userId);
        WritePolicy wp = new WritePolicy();
        wp.expiration = 86400; // 24h TTL
        wp.sendKey    = true;

        long now = Instant.now().toEpochMilli();
        aerospike.put(wp, key,
            new Bin("joinedAt",           now),
            new Bin("lastHeartbeatAt",    now),
            new Bin("watchDurationSeconds", 0L)
        );

        liveSessionRegistry.incrementViewerCount(streamKey);
    }

    /**
     * Updates lastHeartbeatAt and increments watchDurationSeconds.
     * Called by the client every 30 seconds while watching.
     */
    public void heartbeat(String streamKey, UUID userId) {
        Key key = new Key(NS, SET, streamKey + ":" + userId);
        Record rec = aerospike.get(null, key);
        if (rec == null) {
            // Session not found — re-create it (viewer may have reconnected)
            startViewerSession(streamKey, userId);
            return;
        }

        long now  = Instant.now().toEpochMilli();
        long last = rec.getLong("lastHeartbeatAt");
        long deltaSec = Math.max(0, (now - last) / 1000);

        WritePolicy wp = new WritePolicy();
        wp.expiration = 86400;
        wp.sendKey    = true;

        aerospike.operate(wp, key,
            Operation.add(new Bin("watchDurationSeconds", deltaSec)),
            Operation.put(new Bin("lastHeartbeatAt", now))
        );
    }

    /**
     * Ends a viewer session, computing final watch duration and updating the
     * total_watch_minutes on the LiveStream PostgreSQL record.
     */
    public void endViewerSession(String streamKey, UUID userId) {
        Key key = new Key(NS, SET, streamKey + ":" + userId);
        Record rec = aerospike.get(null, key);
        if (rec == null) return;

        long now        = Instant.now().toEpochMilli();
        long last       = rec.getLong("lastHeartbeatAt");
        long existing   = rec.getLong("watchDurationSeconds");
        long deltaSec   = Math.max(0, (now - last) / 1000);
        long totalSec   = existing + deltaSec;

        aerospike.delete(null, key);
        liveSessionRegistry.decrementViewerCount(streamKey);

        // Update totalWatchMinutes in PostgreSQL
        if (totalSec > 0) {
            liveStreamRepository.findByStreamKey(streamKey).ifPresent(stream -> {
                BigDecimal addMinutes = BigDecimal.valueOf(totalSec).divide(BigDecimal.valueOf(60), 2,
                    java.math.RoundingMode.HALF_UP);
                stream.setTotalWatchMinutes(
                    (stream.getTotalWatchMinutes() != null ? stream.getTotalWatchMinutes() : BigDecimal.ZERO)
                    .add(addMinutes));
                liveStreamRepository.save(stream);
            });
        }
    }
}
