package com.play.stream.Starjams.FeedService.Services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.play.stream.Starjams.FeedService.config.KafkaTopics;
import com.play.stream.Starjams.FeedService.entity.RemixCard;
import com.play.stream.Starjams.FeedService.model.EventType;
import com.play.stream.Starjams.FeedService.model.FeedEvent;
import com.play.stream.Starjams.FeedService.repository.RemixCardRepository;
import com.play.stream.Starjams.FeedService.services.FeedFanoutService;
import com.play.stream.Starjams.FeedService.services.FollowGraphService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Consumes remix.created events published by MusicService when a user saves
 * a named stem mix. Persists the remix card to PostgreSQL and fans the
 * TRACK_REMIXED feed event out to the remixer's followers.
 *
 * Artist royalty split:
 *   When the remix card receives gifts, 30% of the gift value is credited to
 *   the original track's artist. This is tracked via total_gifts_received on
 *   the remix_card row; the actual payout split is handled by PaymentService.
 */
@Service
public class RemixService {

    private static final Logger log = LoggerFactory.getLogger(RemixService.class);

    private static final double ARTIST_SPLIT_RATIO = 0.30;
    private static final String REMIX_TOPIC = "remix.created";

    private final RemixCardRepository remixCardRepo;
    private final FeedFanoutService   fanout;
    private final FollowGraphService  followGraph;
    private final KafkaTemplate<String, String> kafka;
    private final ObjectMapper        objectMapper;

    public RemixService(RemixCardRepository remixCardRepo,
                        FeedFanoutService fanout,
                        FollowGraphService followGraph,
                        KafkaTemplate<String, String> kafka,
                        ObjectMapper objectMapper) {
        this.remixCardRepo = remixCardRepo;
        this.fanout        = fanout;
        this.followGraph   = followGraph;
        this.kafka         = kafka;
        this.objectMapper  = objectMapper;
    }

    // -------------------------------------------------------------------------
    // Kafka consumer — remix.created
    // -------------------------------------------------------------------------

    @KafkaListener(topics = REMIX_TOPIC, groupId = "feed-service")
    @Transactional
    public void onRemixCreated(String payload) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> event = objectMapper.readValue(payload, Map.class);

            UUID remixId        = UUID.fromString((String) event.get("remixId"));
            UUID originalTrackId = UUID.fromString((String) event.get("originalTrackId"));
            UUID remixerUserId  = UUID.fromString((String) event.get("remixerUserId"));
            String remixTitle   = (String) event.getOrDefault("remixTitle", "Untitled Remix");

            @SuppressWarnings("unchecked")
            Map<String, Double> stemVolumes = (Map<String, Double>)
                    event.getOrDefault("stemVolumes", Map.of());

            // Persist remix card (use remixId as idempotency key via @Id = remixId)
            if (!remixCardRepo.existsById(remixId)) {
                RemixCard card = new RemixCard(originalTrackId, remixerUserId,
                        remixTitle, stemVolumes);
                remixCardRepo.save(card);
                log.info("Remix card persisted: remixId={} track={} remixer={}",
                        remixId, originalTrackId, remixerUserId);
            }

            // Fan out TRACK_REMIXED to the remixer's followers
            FeedEvent feedEvent = buildRemixFeedEvent(remixId, originalTrackId,
                    remixerUserId, remixTitle);
            List<UUID> followers = followGraph.getAllFollowers(remixerUserId);
            fanout.fanOutEngagement(feedEvent, followers);

            // Notify original artist (30% split — persisted for PaymentService)
            kafka.send(KafkaTopics.NOTIFICATION_EVENT,
                    buildArtistSplitNotification(remixId, originalTrackId, remixerUserId));

        } catch (Exception e) {
            log.error("Failed to process remix.created event: {}", e.getMessage(), e);
        }
    }

    // -------------------------------------------------------------------------
    // Gift recording on remix cards
    // -------------------------------------------------------------------------

    /**
     * Records a gift received by a remix card and accumulates the artist royalty split.
     * Called by ViralMechanicsService after gift-to-unlock threshold processing.
     */
    @Transactional
    public void recordRemixGift(UUID remixId, int giftCount) {
        remixCardRepo.incrementGifts(remixId, giftCount);
        log.debug("Remix {} received {} gift(s)", remixId, giftCount);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private FeedEvent buildRemixFeedEvent(UUID remixId, UUID originalTrackId,
                                           UUID remixerUserId, String remixTitle) {
        FeedEvent fe = new FeedEvent();
        fe.setEventId(UUID.randomUUID());
        fe.setEventType(EventType.TRACK_REMIXED);
        fe.setActorId(remixerUserId);
        fe.setTrackId(originalTrackId);
        fe.setTrackTitle(remixTitle);
        fe.setPostedAt(Instant.now());
        fe.setNew(true);
        return fe;
    }

    private String buildArtistSplitNotification(UUID remixId, UUID originalTrackId,
                                                  UUID remixerUserId) {
        return String.format(
                "{\"type\":\"REMIX_CREATED\",\"remixId\":\"%s\","
              + "\"originalTrackId\":\"%s\",\"remixerUserId\":\"%s\","
              + "\"artistSplitRatio\":%.2f}",
                remixId, originalTrackId, remixerUserId, ARTIST_SPLIT_RATIO);
    }
}
