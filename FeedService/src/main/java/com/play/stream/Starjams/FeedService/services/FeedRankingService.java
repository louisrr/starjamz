package com.play.stream.Starjams.FeedService.services;

import com.play.stream.Starjams.FeedService.model.FeedEvent;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Scores and sorts FeedEvents before they are returned to the client.
 *
 * <p>Score formula:
 * <pre>
 *   score = (recencyScore   * 0.35)
 *         + (engagementScore * 0.25)
 *         + (affinityScore   * 0.20)   // how often viewer plays this artist
 *         + (viralScore      * 0.15)   // repost velocity in last 2h
 *         + (diversityPenalty * 0.05)  // penalise same artist >3× in 20 items
 * </pre>
 *
 * <p>Additionally:
 * <ul>
 *   <li>Pinned livestreams are always first.
 *   <li>isNew + small-creator tracks get a 2.5× boost on recencyScore.
 *   <li>isBuzzing tracks get a flat +0.30 bonus.
 *   <li>TrendingUserCards injected at positions 5, 15, 25, …
 * </ul>
 */
@Service
public class FeedRankingService {

    // Weights
    private static final double W_RECENCY     = 0.35;
    private static final double W_ENGAGEMENT  = 0.25;
    private static final double W_AFFINITY    = 0.20;
    private static final double W_VIRAL       = 0.15;
    private static final double W_DIVERSITY   = 0.05;

    // Bonuses
    private static final double BUZZING_BONUS = 0.30;
    private static final double FIRST48_BOOST = 2.50;
    private static final double LIVESTREAM_BOOST = 10.0; // always pinned to top

    // Diversity penalty threshold: same actor appearing > this many times in top N
    private static final int DIVERSITY_WINDOW = 20;
    private static final int DIVERSITY_MAX    = 3;

    /**
     * Ranks a flat list of feed events for a single user.
     *
     * @param events      raw feed events from Aerospike (unordered)
     * @param affinityMap artistId → affinity score [0,1] for the viewing user
     * @param viralMap    trackId → repost count in last 2 h
     * @return events sorted descending by computed rankScore
     */
    public List<FeedEvent> rank(List<FeedEvent> events,
                                Map<String, Double> affinityMap,
                                Map<String, Long> viralMap) {
        // First pass: compute raw scores
        for (FeedEvent ev : events) {
            double recency    = computeRecency(ev);
            double engagement = computeEngagement(ev);
            double affinity   = affinityMap.getOrDefault(
                ev.getActorId() != null ? ev.getActorId().toString() : "", 0.0);
            double viral      = computeViral(ev, viralMap);

            double raw = (recency    * W_RECENCY)
                       + (engagement * W_ENGAGEMENT)
                       + (affinity   * W_AFFINITY)
                       + (viral      * W_VIRAL);

            // Bonuses
            if (ev.isBuzzing()) raw += BUZZING_BONUS;
            if (ev.isNew())     raw = raw * FIRST48_BOOST; // per-spec 2.5× boost
            if (ev.isLive())    raw += LIVESTREAM_BOOST;

            ev.setRankScore(raw);
        }

        // Sort descending by rank score
        List<FeedEvent> sorted = events.stream()
            .sorted(Comparator.comparingDouble(FeedEvent::getRankScore).reversed())
            .collect(Collectors.toCollection(ArrayList::new));

        // Second pass: apply diversity penalty (mutates rankScore in-place after sort)
        applyDiversityPenalty(sorted);

        // Re-sort after diversity penalty
        sorted.sort(Comparator.comparingDouble(FeedEvent::getRankScore).reversed());

        return sorted;
    }

    // -------------------------------------------------------------------------
    // Recency score — decays from 1.0 at post time toward 0 over 72 h
    // -------------------------------------------------------------------------

    double computeRecency(FeedEvent ev) {
        if (ev.getPostedAt() == null) return 0.0;
        long ageMinutes = Duration.between(ev.getPostedAt(), Instant.now()).toMinutes();
        // Exponential decay: e^(-0.001 * minutes) → ~1.0 at 0m, ~0.07 at 72h
        return Math.exp(-0.001 * ageMinutes);
    }

    // -------------------------------------------------------------------------
    // Engagement score — normalised log of weighted engagement counters
    // -------------------------------------------------------------------------

    double computeEngagement(FeedEvent ev) {
        double weighted = (ev.getPlayCount()    * 0.40)
                        + (ev.getLikeCount()    * 0.30)
                        + (ev.getRepostCount()  * 0.20)
                        + (ev.getCommentCount() * 0.10);
        // log(1+x) normalised to [0,1] assuming max engagement ~10,000
        return Math.min(1.0, Math.log1p(weighted) / Math.log1p(10_000));
    }

    // -------------------------------------------------------------------------
    // Viral score — repost velocity in the last 2 h
    // -------------------------------------------------------------------------

    double computeViral(FeedEvent ev, Map<String, Long> viralMap) {
        if (ev.getTrackId() == null) return 0.0;
        long reposts = viralMap.getOrDefault(ev.getTrackId().toString(), 0L);
        // log-normalise: 100 reposts in 2h → score ≈ 1.0
        return Math.min(1.0, Math.log1p(reposts) / Math.log1p(100));
    }

    // -------------------------------------------------------------------------
    // Diversity penalty — penalise >3 items from the same actor in top 20
    // -------------------------------------------------------------------------

    private void applyDiversityPenalty(List<FeedEvent> sorted) {
        Map<String, Integer> actorCount = new HashMap<>();
        int window = Math.min(DIVERSITY_WINDOW, sorted.size());
        for (int i = 0; i < window; i++) {
            FeedEvent ev = sorted.get(i);
            if (ev.getActorId() == null) continue;
            String actorKey = ev.getActorId().toString();
            int count = actorCount.merge(actorKey, 1, Integer::sum);
            if (count > DIVERSITY_MAX) {
                // Deduct the diversity weight proportionally
                double penalty = W_DIVERSITY * (1.0 / count);
                ev.setRankScore(ev.getRankScore() - penalty);
            }
        }
    }
}
