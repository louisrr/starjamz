package com.play.stream.Starjams.MediaIngressService.services;

import com.aerospike.client.*;
import com.aerospike.client.policy.WritePolicy;
import com.play.stream.Starjams.MediaIngressService.dto.FanOutProgressDto;
import org.springframework.stereotype.Service;

/**
 * Tracks fan-out progress for live stream events in Aerospike.
 *
 * <p>Aerospike schema:
 * <pre>
 *   namespace: fetio
 *   set:       fanout_progress
 *   key:       {streamKey}
 *   bins:      totalFollowers, batchesTotal, batchesCompleted,
 *              feedCardsWritten, failed, status
 * </pre>
 *
 * <p>Written by FeedService (via its own progress reporting) — this service
 * provides the write helpers that MediaIngressService calls to initialise progress,
 * and the read helper used by the admin endpoint.
 */
@Service
public class FanOutProgressService {

    private static final String NS  = "fetio";
    private static final String SET = "fanout_progress";

    private final IAerospikeClient aerospike;

    public FanOutProgressService(IAerospikeClient aerospike) {
        this.aerospike = aerospike;
    }

    public void initProgress(String streamKey, long totalFollowers) {
        Key key = new Key(NS, SET, streamKey);
        WritePolicy wp = new WritePolicy();
        wp.sendKey = true;
        wp.expiration = 86400; // 24h TTL

        long batchesTotal = totalFollowers > 0 ? (long) Math.ceil(totalFollowers / 1000.0) : 1;

        aerospike.put(wp, key,
            new Bin("totalFollowers",   totalFollowers),
            new Bin("batchesTotal",     batchesTotal),
            new Bin("batchesCompleted", 0L),
            new Bin("feedCardsWritten", 0L),
            new Bin("failed",           0L),
            new Bin("status",           "IN_PROGRESS")
        );
    }

    public void recordBatchComplete(String streamKey, long cardsWritten) {
        Key key = new Key(NS, SET, streamKey);
        WritePolicy wp = new WritePolicy();
        wp.sendKey = true;

        Record after = aerospike.operate(wp, key,
            Operation.add(new Bin("batchesCompleted", 1L)),
            Operation.add(new Bin("feedCardsWritten", cardsWritten)),
            Operation.get("batchesCompleted"),
            Operation.get("batchesTotal")
        );

        if (after != null) {
            long completed = after.getLong("batchesCompleted");
            long total     = after.getLong("batchesTotal");
            if (completed >= total) {
                aerospike.put(wp, key, new Bin("status", "DONE"));
            }
        }
    }

    public void recordFailure(String streamKey, long failedCount) {
        Key key = new Key(NS, SET, streamKey);
        WritePolicy wp = new WritePolicy();
        wp.sendKey = true;
        aerospike.operate(wp, key,
            Operation.add(new Bin("failed", failedCount)),
            Operation.put(new Bin("status", "PARTIAL"))
        );
    }

    public FanOutProgressDto getProgress(String streamKey) {
        Key key = new Key(NS, SET, streamKey);
        Record rec = aerospike.get(null, key);

        FanOutProgressDto dto = new FanOutProgressDto();
        dto.setStreamKey(streamKey);

        if (rec == null) {
            dto.setStatus("UNKNOWN");
            return dto;
        }

        dto.setTotalFollowers(rec.getLong("totalFollowers"));
        dto.setBatchesTotal(rec.getLong("batchesTotal"));
        dto.setBatchesCompleted(rec.getLong("batchesCompleted"));
        dto.setFeedCardsWritten(rec.getLong("feedCardsWritten"));
        dto.setFailed(rec.getLong("failed"));
        dto.setStatus(rec.getString("status"));
        return dto;
    }
}
