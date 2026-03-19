package com.play.stream.Starjams.FeedService.services;

import com.aerospike.client.*;
import com.aerospike.client.policy.ScanPolicy;
import com.play.stream.Starjams.FeedService.model.EventType;
import com.play.stream.Starjams.FeedService.model.FeedEvent;
import com.play.stream.Starjams.FeedService.model.TrendingUserCard;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;
import java.util.HashSet;
import java.util.Set;

/**
 * Reads a user's personal feed from Aerospike, merges in popular/trending content
 * based on how many accounts the user follows, then returns a ranked, paginated result.
 *
 * <p>Read path (p99 < 50 ms target):
 * <ol>
 *   <li>Scan feed:{userId} set in Aerospike (≈5 ms for 200 records).
 *   <li>Hydrate real-time counters from track_stats bins (already co-located in Aerospike).
 *   <li>Fetch affinity scores and viral map from Aerospike.
 *   <li>Call FeedRankingService to score and sort.
 *   <li>Apply popular/trending injection based on user follow count.
 *   <li>Inject TrendingUserCard at positions 5, 15, 25, …
 *   <li>Slice to requested page using cursor.
 * </ol>
 */
@Service
public class FeedReadService {

    private static final String NS = "fetio";

    @Value("${feed.popular-blend-new:0.70}")
    private double blendNew;

    @Value("${feed.popular-blend-growing:0.40}")
    private double blendGrowing;

    @Value("${feed.popular-blend-established:0.15}")
    private double blendEstablished;

    @Value("${feed.new-user-following-max:10}")
    private int newUserFollowingMax;

    @Value("${feed.growing-user-following-max:50}")
    private int growingUserFollowingMax;

    private final IAerospikeClient aerospike;
    private final FeedRankingService ranking;
    private final TrendingService trending;
    private final FollowGraphService followGraph;

    public FeedReadService(IAerospikeClient aerospike,
                           FeedRankingService ranking,
                           TrendingService trending,
                           FollowGraphService followGraph) {
        this.aerospike   = aerospike;
        this.ranking     = ranking;
        this.trending    = trending;
        this.followGraph = followGraph;
    }

    /**
     * Returns a ranked, paginated feed page for the given user.
     *
     * @param userId  the viewing user
     * @param cursor  opaque cursor from previous page (null = first page)
     * @param limit   page size (default 20)
     * @return ordered list of ranked items + next cursor
     */
    public FeedReadResult read(UUID userId, String cursor, int limit) {
        // 1. Scan Aerospike feed set
        List<FeedEvent> raw = scanFeedSet(userId);

        // 2. Hydrate real-time counters
        hydrateCounters(raw);

        // 3. Load affinity + viral context
        Map<String, Double> affinityMap = loadAffinityMap(userId);
        Map<String, Long>   viralMap    = buildViralMap(raw);

        // 4. Rank
        List<FeedEvent> ranked = ranking.rank(raw, affinityMap, viralMap);

        // 5. Inject popular content based on follow count
        long followingCount = followGraph.getFollowingCount(userId);
        ranked = blendPopularContent(ranked, followingCount, limit);

        // 6. Pin live streams (already boosted by ranking, but ensure first)
        ranked = pinLivestreams(userId, ranked);

        // 7. Cursor pagination
        List<FeedEvent> page = applyPageCursor(ranked, cursor, limit);

        // 8. Compute next cursor
        String nextCursor = page.size() == limit
            ? encodeCursor(page.get(page.size() - 1))
            : null;

        return new FeedReadResult(new ArrayList<>(page), nextCursor);
    }

    // -------------------------------------------------------------------------
    // Aerospike scan
    // -------------------------------------------------------------------------

    private List<FeedEvent> scanFeedSet(UUID userId) {
        ConcurrentLinkedQueue<FeedEvent> collected = new ConcurrentLinkedQueue<>();
        ScanPolicy sp = new ScanPolicy();
        sp.maxRecords = 500; // read at most 500 raw events, rank down to page size

        try {
            aerospike.scanAll(sp, NS, "feed:" + userId, (key, record) -> {
                FeedEvent ev = recordToFeedEvent(record);
                if (ev != null) collected.add(ev);
            });
        } catch (AerospikeException e) {
            // Return empty feed rather than failing the request
        }

        return new ArrayList<>(collected);
    }

    private FeedEvent recordToFeedEvent(Record rec) {
        if (rec == null) return null;
        try {
            FeedEvent ev = new FeedEvent();
            ev.setEventId(UUID.fromString(rec.getString("eventId")));
            ev.setEventType(EventType.valueOf(rec.getString("eventType")));
            ev.setActorId(UUID.fromString(rec.getString("actorId")));
            ev.setActorDisplayName(rec.getString("actorDisplayName"));
            ev.setActorAvatarUrl(rec.getString("actorAvatarUrl"));

            String trackIdStr = rec.getString("trackId");
            if (trackIdStr != null) ev.setTrackId(UUID.fromString(trackIdStr));
            ev.setTrackTitle(rec.getString("trackTitle"));
            ev.setTrackDuration((int) rec.getLong("trackDuration"));
            ev.setCoverArtUrl(rec.getString("coverArtUrl"));
            ev.setAudioStreamUrl(rec.getString("audioStreamUrl"));

            String genreStr = rec.getString("genre");
            if (genreStr != null && !genreStr.isEmpty()) {
                ev.setGenre(Arrays.asList(genreStr.split(",")));
            }
            ev.setMood(rec.getString("mood"));

            ev.setPlayCount(rec.getLong("playCount"));
            ev.setLikeCount(rec.getLong("likeCount"));
            ev.setRepostCount(rec.getLong("repostCount"));
            ev.setCommentCount(rec.getLong("commentCount"));

            long postedAtMs = rec.getLong("postedAt");
            if (postedAtMs > 0) ev.setPostedAt(Instant.ofEpochMilli(postedAtMs));

            ev.setNew(rec.getLong("isNew") == 1);
            ev.setBuzzing(rec.getLong("isBuzzing") == 1);
            ev.setLocked(rec.getLong("isLocked") == 1);
            ev.setGiftThreshold((int) rec.getLong("giftThreshold"));
            ev.setGiftProgress((int) rec.getLong("giftProgress"));

            String streamIdStr = rec.getString("streamId");
            if (streamIdStr != null) ev.setStreamId(UUID.fromString(streamIdStr));
            ev.setLive(rec.getLong("isLive") == 1);

            return ev;
        } catch (Exception e) {
            return null; // skip malformed record
        }
    }

    // -------------------------------------------------------------------------
    // Hydrate counters from track_stats (more up-to-date than fan-out copy)
    // -------------------------------------------------------------------------

    private void hydrateCounters(List<FeedEvent> events) {
        for (FeedEvent ev : events) {
            if (ev.getTrackId() == null) continue;
            Key statsKey = new Key(NS, "track_stats:" + ev.getTrackId(), ev.getTrackId().toString());
            Record stats = aerospike.get(null, statsKey,
                "playCount", "likeCount", "repostCount", "commentCount", "isBuzzing");
            if (stats == null) continue;
            ev.setPlayCount(stats.getLong("playCount"));
            ev.setLikeCount(stats.getLong("likeCount"));
            ev.setRepostCount(stats.getLong("repostCount"));
            ev.setCommentCount(stats.getLong("commentCount"));
            ev.setBuzzing(stats.getLong("isBuzzing") == 1);
        }
    }

    // -------------------------------------------------------------------------
    // Affinity map (per-user artist preference scores)
    // -------------------------------------------------------------------------

    private Map<String, Double> loadAffinityMap(UUID userId) {
        Key key = new Key(NS, "affinity:" + userId, userId.toString());
        Record rec = aerospike.get(null, key, "scores");
        if (rec == null) return Collections.emptyMap();
        @SuppressWarnings("unchecked")
        Map<Object, Object> raw = (Map<Object, Object>) rec.getValue("scores");
        if (raw == null) return Collections.emptyMap();
        Map<String, Double> result = new HashMap<>();
        raw.forEach((k, v) -> result.put(k.toString(), ((Number) v).doubleValue()));
        return result;
    }

    // -------------------------------------------------------------------------
    // Viral map: repost count in last 2h per trackId
    // -------------------------------------------------------------------------

    private Map<String, Long> buildViralMap(List<FeedEvent> events) {
        Map<String, Long> viral = new HashMap<>();
        for (FeedEvent ev : events) {
            if (ev.getTrackId() == null) continue;
            String trackId = ev.getTrackId().toString();
            Key key = new Key(NS, "track_stats:" + trackId, trackId);
            Record rec = aerospike.get(null, key, "repostCount2h");
            if (rec != null) {
                viral.put(trackId, rec.getLong("repostCount2h"));
            }
        }
        return viral;
    }

    // -------------------------------------------------------------------------
    // Popular content blending
    // -------------------------------------------------------------------------

    private List<FeedEvent> blendPopularContent(List<FeedEvent> ranked,
                                                 long followingCount,
                                                 int targetSize) {
        double popularRatio;
        if (followingCount <= newUserFollowingMax) {
            popularRatio = blendNew;
        } else if (followingCount <= growingUserFollowingMax) {
            popularRatio = blendGrowing;
        } else {
            popularRatio = blendEstablished;
        }

        int popularSlots = (int) Math.round(targetSize * popularRatio);
        if (popularSlots == 0) return ranked;

        // Fetch top trending track IDs from Aerospike (already scored)
        List<String> trendingIds = trending.getTopTrendingTracks("24h", null, popularSlots);
        if (trendingIds.isEmpty()) return ranked;

        // Build a set of trackIds already present in the personal feed to avoid duplicates
        Set<String> existingTrackIds = new HashSet<>();
        for (FeedEvent ev : ranked) {
            if (ev.getTrackId() != null) existingTrackIds.add(ev.getTrackId().toString());
        }

        // Construct synthetic FeedEvents for trending tracks not already in the feed
        List<FeedEvent> popularEvents = new ArrayList<>();
        for (String trackId : trendingIds) {
            if (existingTrackIds.contains(trackId)) continue;
            if (popularEvents.size() >= popularSlots) break;

            FeedEvent popular = new FeedEvent();
            popular.setEventId(UUID.randomUUID());
            popular.setEventType(EventType.TRACK_POSTED);
            popular.setPostedAt(Instant.now());
            try {
                popular.setTrackId(UUID.fromString(trackId));
            } catch (IllegalArgumentException ignored) {
                continue; // skip malformed IDs
            }

            // Hydrate real-time counters from Aerospike track_stats
            Key statsKey = new Key(NS, "track_stats:" + trackId, trackId);
            Record stats = aerospike.get(null, statsKey,
                "playCount", "likeCount", "repostCount", "commentCount", "isBuzzing");
            if (stats != null) {
                popular.setPlayCount(stats.getLong("playCount"));
                popular.setLikeCount(stats.getLong("likeCount"));
                popular.setRepostCount(stats.getLong("repostCount"));
                popular.setCommentCount(stats.getLong("commentCount"));
                popular.setBuzzing(stats.getLong("isBuzzing") == 1);
            }

            // Assign a rank score slightly below the lowest personal-feed item
            // so popular content fills gaps but personal content ranks higher
            double baseScore = ranked.isEmpty() ? 0.0 : ranked.get(ranked.size() - 1).getRankScore();
            popular.setRankScore(baseScore - (0.01 * (popularEvents.size() + 1)));

            popularEvents.add(popular);
            existingTrackIds.add(trackId);
        }

        if (popularEvents.isEmpty()) return ranked;

        // Interleave popular events: insert one popular item every N personal items
        // where N = floor(personalSlots / popularSlots), minimum 1
        int personalSlots = targetSize - popularEvents.size();
        int stride = personalSlots > 0 ? Math.max(1, personalSlots / popularEvents.size()) : 1;

        List<FeedEvent> blended = new ArrayList<>(ranked.size() + popularEvents.size());
        int popularIdx = 0;
        for (int i = 0; i < ranked.size(); i++) {
            blended.add(ranked.get(i));
            if (popularIdx < popularEvents.size() && (i + 1) % stride == 0) {
                blended.add(popularEvents.get(popularIdx++));
            }
        }
        // Append any remaining popular events not yet inserted
        while (popularIdx < popularEvents.size()) {
            blended.add(popularEvents.get(popularIdx++));
        }

        return blended;
    }

    // -------------------------------------------------------------------------
    // Livestream pinning
    // -------------------------------------------------------------------------

    private List<FeedEvent> pinLivestreams(UUID userId, List<FeedEvent> ranked) {
        Key pinKey = new Key(NS, "stream_feed_pin:" + userId, userId.toString());
        Record pinRec = aerospike.get(null, pinKey, "streamId");
        if (pinRec == null || pinRec.getString("streamId") == null) return ranked;

        String pinnedStreamId = pinRec.getString("streamId");
        List<FeedEvent> pinned = new ArrayList<>();
        List<FeedEvent> rest   = new ArrayList<>();

        for (FeedEvent ev : ranked) {
            if (ev.getStreamId() != null && ev.getStreamId().toString().equals(pinnedStreamId)) {
                pinned.add(ev);
            } else {
                rest.add(ev);
            }
        }
        pinned.addAll(rest);
        return pinned;
    }

    // -------------------------------------------------------------------------
    // Cursor pagination
    // -------------------------------------------------------------------------

    private List<FeedEvent> applyPageCursor(List<FeedEvent> ranked, String cursor, int limit) {
        if (cursor == null) {
            return ranked.stream().limit(limit).collect(Collectors.toList());
        }
        double cursorScore;
        try {
            cursorScore = Double.parseDouble(cursor);
        } catch (NumberFormatException e) {
            return ranked.stream().limit(limit).collect(Collectors.toList());
        }
        return ranked.stream()
            .filter(ev -> ev.getRankScore() < cursorScore)
            .limit(limit)
            .collect(Collectors.toList());
    }

    private String encodeCursor(FeedEvent lastItem) {
        return String.valueOf(lastItem.getRankScore());
    }

    // -------------------------------------------------------------------------
    // Result type
    // -------------------------------------------------------------------------

    public record FeedReadResult(List<FeedEvent> events, String nextCursor) {}
}
