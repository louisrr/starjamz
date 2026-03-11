package com.play.stream.Starjams.FeedService.services;

import com.aerospike.client.*;
import com.aerospike.client.cdt.MapOperation;
import com.aerospike.client.cdt.MapOrder;
import com.aerospike.client.cdt.MapPolicy;
import com.aerospike.client.cdt.MapWriteMode;
import com.aerospike.client.policy.WritePolicy;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Tracks per-user artist affinity scores in Aerospike.
 *
 * <p>Affinity is updated on every qualifying play (>80% completion).
 * Score is a simple exponential moving average: new = old * 0.9 + 0.1.
 *
 * <p>Aerospike set: {@code affinity:{userId}}
 * <pre>
 *   key:  userId
 *   bin:  "scores" → Map<String(artistId), Double(affinityScore)>
 * </pre>
 */
@Service
public class AffinityService {

    private static final String NS = "fetio";
    private static final MapPolicy MAP_POLICY =
        new MapPolicy(MapOrder.KEY_ORDERED, MapWriteMode.UPDATE);

    private final IAerospikeClient aerospike;

    public AffinityService(IAerospikeClient aerospike) {
        this.aerospike = aerospike;
    }

    /**
     * Records a play interaction and updates the affinity score for {@code artistId}.
     * Only call when completion percentage > 80%.
     */
    public void recordPlay(UUID userId, UUID artistId) {
        Key key = new Key(NS, "affinity:" + userId, userId.toString());
        String artistKey = artistId.toString();

        // Read current score
        Record rec = aerospike.operate(null, key,
            MapOperation.getByKey("scores", Value.get(artistKey),
                com.aerospike.client.cdt.MapReturnType.VALUE));

        double current = 0.0;
        if (rec != null && rec.getValue("scores") != null) {
            Object val = rec.getValue("scores");
            current = ((Number) val).doubleValue();
        }

        // Exponential moving average update
        double updated = current * 0.9 + 0.1;

        WritePolicy wp = new WritePolicy();
        wp.sendKey = true;
        aerospike.operate(wp, key,
            MapOperation.put(MAP_POLICY, "scores",
                Value.get(artistKey), Value.get(updated)));
    }

    /**
     * Returns the affinity score for a specific artist [0.0, 1.0].
     */
    public double getAffinity(UUID userId, UUID artistId) {
        Key key = new Key(NS, "affinity:" + userId, userId.toString());
        Record rec = aerospike.operate(null, key,
            MapOperation.getByKey("scores", Value.get(artistId.toString()),
                com.aerospike.client.cdt.MapReturnType.VALUE));
        if (rec == null || rec.getValue("scores") == null) return 0.0;
        return ((Number) rec.getValue("scores")).doubleValue();
    }
}
