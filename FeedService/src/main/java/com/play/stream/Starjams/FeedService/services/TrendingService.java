package com.play.stream.Starjams.FeedService.services;

import com.aerospike.client.*;
import com.aerospike.client.cdt.MapOperation;
import com.aerospike.client.cdt.MapOrder;
import com.aerospike.client.cdt.MapPolicy;
import com.aerospike.client.cdt.MapReturnType;
import com.aerospike.client.cdt.MapWriteMode;
import com.aerospike.client.policy.WritePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

/**
 * Maintains real-time trending sorted sets in Aerospike.
 *
 * <p>Trending score formula (per spec):
 * <pre>
 *   score = ((plays*0.4) + (likes*0.3) + (reposts*0.2) + (comments*0.1))
 *         * e^(-λt)    where λ=0.05, t=hours since post
 * </pre>
 *
 * <p>Aerospike sets used:
 * <pre>
 *   trending_tracks                — global top-100, map trackId→score
 *   trending_tracks:{timeWindow}   — per-window snapshots (1h, 6h, 24h)
 *   track_stats:{trackId}          — raw engagement counters + postedAt
 *   trending_users:global          — top-100 users, refreshed every 5 min
 *   trending_users:{genre}         — top-50 per genre
 *   user_growth_stats:{userId}     — follower/play/repost velocity
 * </pre>
 */
@Service
public class TrendingService {

    private static final Logger log = LoggerFactory.getLogger(TrendingService.class);
    private static final String NS = "fetio";

    // Trending score weights
    private static final double W_PLAYS    = 0.4;
    private static final double W_LIKES    = 0.3;
    private static final double W_REPOSTS  = 0.2;
    private static final double W_COMMENTS = 0.1;

    @Value("${feed.trending-decay-lambda:0.05}")
    private double lambda;

    private static final MapPolicy MAP_POLICY =
        new MapPolicy(MapOrder.KEY_VALUE_ORDERED, MapWriteMode.UPDATE);

    private final IAerospikeClient aerospike;

    public TrendingService(IAerospikeClient aerospike) {
        this.aerospike = aerospike;
    }

    // -------------------------------------------------------------------------
    // Engagement counter update (called on every play/like/repost/comment)
    // -------------------------------------------------------------------------

    public void incrementCounter(String trackId, String counterBin) {
        Key key = new Key(NS, "track_stats:" + trackId, trackId);
        WritePolicy wp = new WritePolicy();
        wp.sendKey = true;
        aerospike.operate(wp, key, Operation.add(new Bin(counterBin, 1L)));
    }

    /**
     * Recomputes trending score for a track after engagement, then updates
     * the global trending sorted map. Called on every qualifying event.
     */
    public void updateTrendingScore(String trackId) {
        Key statsKey = new Key(NS, "track_stats:" + trackId, trackId);
        Record stats = aerospike.get(null, statsKey,
            "playCount", "likeCount", "repostCount", "commentCount", "postedAt");
        if (stats == null) return;

        long plays    = stats.getLong("playCount");
        long likes    = stats.getLong("likeCount");
        long reposts  = stats.getLong("repostCount");
        long comments = stats.getLong("commentCount");
        long postedAt = stats.getLong("postedAt");  // epoch ms

        double rawScore = (plays * W_PLAYS) + (likes * W_LIKES)
                        + (reposts * W_REPOSTS) + (comments * W_COMMENTS);
        double hoursOld = (Instant.now().toEpochMilli() - postedAt) / 3_600_000.0;
        double decayedScore = rawScore * Math.exp(-lambda * hoursOld);

        // Write decayed score into global trending map
        writeToTrendingMap("global", trackId, decayedScore);
    }

    /**
     * Returns top-N trending track IDs for the given time window.
     * @param timeWindow "1h" | "6h" | "24h"
     * @param genre      optional genre filter (null = all genres)
     * @param limit      max results
     */
    public List<String> getTopTrendingTracks(String timeWindow, String genre, int limit) {
        String mapKey = genre != null ? genre : "global";
        Key key = new Key(NS, "trending_tracks", mapKey);
        Record rec = aerospike.operate(null, key,
            MapOperation.getByRankRange("scores", -limit, limit, MapReturnType.KEY));
        if (rec == null) return Collections.emptyList();
        List<?> keys = (List<?>) rec.getValue("scores");
        if (keys == null) return Collections.emptyList();
        List<String> result = new ArrayList<>();
        for (Object o : keys) result.add(o.toString());
        Collections.reverse(result);   // highest rank first
        return result;
    }

    /**
     * Returns the repost count for a track in the last 2 hours.
     * Used by FeedRankingService to compute viralScore.
     */
    public long getRecentRepostCount(String trackId) {
        Key key = new Key(NS, "track_stats:" + trackId, trackId);
        Record rec = aerospike.get(null, key, "repostCount2h");
        if (rec == null) return 0;
        return rec.getLong("repostCount2h");
    }

    // -------------------------------------------------------------------------
    // Initialise track stats record on first post
    // -------------------------------------------------------------------------

    public void initTrackStats(String trackId, Instant postedAt) {
        Key key = new Key(NS, "track_stats:" + trackId, trackId);
        WritePolicy wp = new WritePolicy();
        wp.sendKey = true;
        aerospike.put(wp, key,
            new Bin("playCount",     0L),
            new Bin("likeCount",     0L),
            new Bin("repostCount",   0L),
            new Bin("commentCount",  0L),
            new Bin("repostCount2h", 0L),
            new Bin("postedAt",      postedAt.toEpochMilli()));
    }

    // -------------------------------------------------------------------------
    // Internal: write score to Aerospike trending map
    // -------------------------------------------------------------------------

    private void writeToTrendingMap(String mapKey, String trackId, double score) {
        Key key = new Key(NS, "trending_tracks", mapKey);
        WritePolicy wp = new WritePolicy();
        aerospike.operate(wp, key,
            MapOperation.put(MAP_POLICY, "scores",
                Value.get(trackId), Value.get(score)));
    }
}
