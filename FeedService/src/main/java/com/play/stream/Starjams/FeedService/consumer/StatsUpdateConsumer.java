package com.play.stream.Starjams.FeedService.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.play.stream.Starjams.FeedService.config.KafkaTopics;
import com.play.stream.Starjams.FeedService.dto.TrackEngagedEvent;
import com.play.stream.Starjams.FeedService.model.EventType;
import com.play.stream.Starjams.FeedService.services.AffinityService;
import com.play.stream.Starjams.FeedService.services.TrendingService;
import com.play.stream.Starjams.FeedService.services.ViralMechanicsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumes {@code track.engaged} events to:
 * <ol>
 *   <li>Atomically increment the appropriate Aerospike engagement counter.
 *   <li>Recompute the track's trending score (recency-decayed).
 *   <li>Update per-user artist affinity on qualifying plays (>80% completion).
 *   <li>Check buzzing threshold (100+ plays in 6h).
 *   <li>Record play streak for follow-nudge notifications.
 * </ol>
 *
 * <p>Trending scores are updated within 10 seconds of any qualifying engagement
 * event — satisfying the acceptance criterion.
 */
@Component
public class StatsUpdateConsumer {

    private static final Logger log = LoggerFactory.getLogger(StatsUpdateConsumer.class);

    private final TrendingService    trending;
    private final AffinityService    affinity;
    private final ViralMechanicsService viral;
    private final ObjectMapper       objectMapper;

    public StatsUpdateConsumer(TrendingService trending,
                                AffinityService affinity,
                                ViralMechanicsService viral,
                                ObjectMapper objectMapper) {
        this.trending     = trending;
        this.affinity     = affinity;
        this.viral        = viral;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = KafkaTopics.TRACK_ENGAGED, groupId = "feed-service-stats")
    public void onEngagement(String payload) {
        TrackEngagedEvent event;
        try {
            event = objectMapper.readValue(payload, TrackEngagedEvent.class);
        } catch (Exception e) {
            log.error("Malformed track.engaged payload: {}", e.getMessage());
            return;
        }

        if (event.getTrackId() == null || event.getActorId() == null) return;

        String trackId = event.getTrackId().toString();
        EventType type = event.getEngagementType();

        // 1. Increment the appropriate counter
        switch (type) {
            case TRACK_PLAYED  -> {
                trending.incrementCounter(trackId, "playCount");
                if (event.getCompletionPct() > 0.8) {
                    trending.incrementCounter(trackId, "playCount6h");
                }
            }
            case TRACK_LIKED   -> trending.incrementCounter(trackId, "likeCount");
            case TRACK_REPOSTED -> {
                trending.incrementCounter(trackId, "repostCount");
                trending.incrementCounter(trackId, "repostCount2h");
            }
            default -> {} // comment, etc. — handled elsewhere
        }

        // 2. Recompute trending score (within 10 s of event arrival)
        try {
            trending.updateTrendingScore(trackId);
        } catch (Exception e) {
            log.warn("Could not update trending score for {}: {}", trackId, e.getMessage());
        }

        // 3. Update affinity on qualifying plays
        if (type == EventType.TRACK_PLAYED && event.getCompletionPct() > 0.8) {
            try {
                affinity.recordPlay(event.getActorId(), event.getTrackId());
            } catch (Exception e) {
                log.warn("Could not update affinity: {}", e.getMessage());
            }

            // 4. Play streak for follow-nudge
            try {
                viral.recordPlay(event.getActorId(), event.getTrackId());
            } catch (Exception e) {
                log.warn("Could not update play streak: {}", e.getMessage());
            }
        }

        // 5. Buzzing threshold check on every play
        if (type == EventType.TRACK_PLAYED) {
            try {
                viral.checkBuzzingThreshold(trackId);
            } catch (Exception e) {
                log.warn("Buzzing check failed for {}: {}", trackId, e.getMessage());
            }
        }
    }
}
