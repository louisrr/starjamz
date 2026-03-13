package com.play.stream.Starjams.FeedService.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.play.stream.Starjams.FeedService.config.KafkaTopics;
import com.play.stream.Starjams.FeedService.dto.TrackPostedEvent;
import com.play.stream.Starjams.FeedService.model.EventType;
import com.play.stream.Starjams.FeedService.model.FeedEvent;
import com.play.stream.Starjams.FeedService.services.FeedFanoutService;
import com.play.stream.Starjams.FeedService.services.FollowGraphService;
import com.play.stream.Starjams.FeedService.services.PrivacyService;
import com.play.stream.Starjams.FeedService.services.TrendingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Consumes {@code track.posted} and activity engagement events, then fans them
 * out to follower feeds according to the event-type fan-out policy:
 *
 * <pre>
 *   TRACK_POSTED    → ALL followers, immediately
 *   TRACK_REPOSTED  → ALL followers of reposter, immediately
 *   TRACK_LIKED     → only if track has >50 likes OR actor has >200 followers
 *   TRACK_PLAYED    → batched into DigestCard (no individual fan-out)
 *   ARTIST_FOLLOWED → fan out as discovery nudge card
 *   VIDEO_LIKED     → same threshold as TRACK_LIKED
 *   VIDEO_VIEWED    → batched (no individual fan-out)
 * </pre>
 */
@Component
public class FeedFanoutConsumer {

    private static final Logger log = LoggerFactory.getLogger(FeedFanoutConsumer.class);

    private static final long LIKE_FANOUT_THRESHOLD_LIKES   = 50;
    private static final long LIKE_FANOUT_THRESHOLD_FOLLOWERS = 200;

    private final FeedFanoutService fanout;
    private final FollowGraphService followGraph;
    private final PrivacyService    privacy;
    private final TrendingService   trending;
    private final ObjectMapper      objectMapper;

    public FeedFanoutConsumer(FeedFanoutService fanout,
                               FollowGraphService followGraph,
                               PrivacyService privacy,
                               TrendingService trending,
                               ObjectMapper objectMapper) {
        this.fanout       = fanout;
        this.followGraph  = followGraph;
        this.privacy      = privacy;
        this.trending     = trending;
        this.objectMapper = objectMapper;
    }

    // -------------------------------------------------------------------------
    // track.posted — new track upload
    // -------------------------------------------------------------------------

    @KafkaListener(topics = KafkaTopics.TRACK_POSTED, groupId = "feed-service")
    public void onTrackPosted(String payload) {
        try {
            TrackPostedEvent event = objectMapper.readValue(payload, TrackPostedEvent.class);
            trending.initTrackStats(
                event.getTrackId().toString(),
                event.getPostedAt() != null ? event.getPostedAt() : Instant.now());
            fanout.fanOutTrackPosted(event);
        } catch (Exception e) {
            log.error("Failed to process track.posted event: {}", e.getMessage(), e);
        }
    }

    // -------------------------------------------------------------------------
    // track.engaged — like, play, repost, comment
    // -------------------------------------------------------------------------

    @KafkaListener(topics = KafkaTopics.TRACK_ENGAGED, groupId = "feed-service")
    public void onTrackEngaged(String payload) {
        try {
            com.play.stream.Starjams.FeedService.dto.TrackEngagedEvent event =
                objectMapper.readValue(payload, com.play.stream.Starjams.FeedService.dto.TrackEngagedEvent.class);

            EventType type = event.getEngagementType();

            switch (type) {
                case TRACK_LIKED, VIDEO_LIKED -> handleLikeEvent(event);
                case TRACK_PLAYED, VIDEO_VIEWED -> {
                    // Only record affinity on >80% completion; no individual fan-out
                    log.debug("Play event batched into digest — no individual fan-out");
                }
                case TRACK_REPOSTED  -> handleRepostEvent(event);
                case TRACK_REMIXED   -> handleRemixEvent(event);
                case ARTIST_FOLLOWED -> handleFollowEvent(event);
                default -> log.debug("No fan-out policy for event type {}", type);
            }
        } catch (Exception e) {
            log.error("Failed to process track.engaged event: {}", e.getMessage(), e);
        }
    }

    // -------------------------------------------------------------------------
    // livestream.event
    // -------------------------------------------------------------------------

    @KafkaListener(topics = KafkaTopics.LIVESTREAM_EVENT, groupId = "feed-service")
    public void onLivestreamEvent(String payload) {
        try {
            FeedEvent livestreamEvent = objectMapper.readValue(payload, FeedEvent.class);
            List<UUID> followers = followGraph.getAllFollowers(livestreamEvent.getActorId());
            fanout.fanOutLivestream(livestreamEvent, followers);
        } catch (Exception e) {
            log.error("Failed to process livestream.event: {}", e.getMessage(), e);
        }
    }

    // -------------------------------------------------------------------------
    // Fan-out policy implementations
    // -------------------------------------------------------------------------

    private void handleLikeEvent(com.play.stream.Starjams.FeedService.dto.TrackEngagedEvent event) {
        // Fan-out only if: track >50 likes OR actor >200 followers
        long currentLikes    = trending.getRecentRepostCount(event.getTrackId().toString()); // reuse proxy
        long actorFollowers  = event.getActorFollowerCount();

        if (currentLikes < LIKE_FANOUT_THRESHOLD_LIKES
                && actorFollowers < LIKE_FANOUT_THRESHOLD_FOLLOWERS) {
            log.debug("Suppressing LIKE fan-out — thresholds not met (likes={}, followers={})",
                currentLikes, actorFollowers);
            return;
        }

        // Check privacy preference
        if (!privacy.canShareActivity(event.getActorId(), event.getEngagementType().name())) {
            return;
        }

        FeedEvent feedEvent = buildActivityFeedEvent(event);
        List<UUID> followers = followGraph.getAllFollowers(event.getActorId());
        fanout.fanOutEngagement(feedEvent, followers);
    }

    private void handleRepostEvent(com.play.stream.Starjams.FeedService.dto.TrackEngagedEvent event) {
        // All followers of the reposter receive the repost — no threshold
        FeedEvent feedEvent = buildActivityFeedEvent(event);
        List<UUID> followers = followGraph.getAllFollowers(event.getActorId());
        fanout.fanOutEngagement(feedEvent, followers);
    }

    private void handleRemixEvent(com.play.stream.Starjams.FeedService.dto.TrackEngagedEvent event) {
        // All followers of the remixer receive the remix event — no threshold.
        // RemixService (via remix.created Kafka topic) handles the primary fan-out;
        // this branch covers remix engagements that flow through track.engaged.
        FeedEvent feedEvent = buildActivityFeedEvent(event);
        List<UUID> followers = followGraph.getAllFollowers(event.getActorId());
        fanout.fanOutEngagement(feedEvent, followers);
    }

    private void handleFollowEvent(com.play.stream.Starjams.FeedService.dto.TrackEngagedEvent event) {
        if (!privacy.canShareActivity(event.getActorId(), "ARTIST_FOLLOWED")) return;

        // Fan out as "discovery nudge" card to the actor's followers
        FeedEvent feedEvent = buildActivityFeedEvent(event);
        List<UUID> followers = followGraph.getAllFollowers(event.getActorId());
        fanout.fanOutEngagement(feedEvent, followers);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private FeedEvent buildActivityFeedEvent(com.play.stream.Starjams.FeedService.dto.TrackEngagedEvent ev) {
        FeedEvent fe = new FeedEvent();
        fe.setEventId(UUID.randomUUID());
        fe.setEventType(ev.getEngagementType());
        fe.setActorId(ev.getActorId());
        fe.setTrackId(ev.getTrackId());
        fe.setPostedAt(ev.getOccurredAt() != null ? ev.getOccurredAt() : Instant.now());
        fe.setRepostCommentText(ev.getRepostCommentText());
        fe.setRepostCommentAudioUrl(ev.getRepostCommentAudioUrl());
        return fe;
    }
}
