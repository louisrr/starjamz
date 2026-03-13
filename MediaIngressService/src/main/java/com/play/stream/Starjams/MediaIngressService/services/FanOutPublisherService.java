package com.play.stream.Starjams.MediaIngressService.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.play.stream.Starjams.MediaIngressService.dto.LivestreamKafkaEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Publishes livestream lifecycle events to Kafka topics.
 *
 * <p>Published events:
 * <ul>
 *   <li>{@code livestream.event} — consumed by FeedFanoutConsumer in FeedService,
 *       which fans out to all followers. The existing {@code onLivestreamEvent} handler
 *       deserialises the payload as {@code FeedEvent}, so field names must match.</li>
 *   <li>{@code notification.event} — consumed by NotificationService for push delivery.</li>
 * </ul>
 */
@Service
public class FanOutPublisherService {

    private static final Logger log = LoggerFactory.getLogger(FanOutPublisherService.class);

    private static final String TOPIC_LIVESTREAM    = "livestream.event";
    private static final String TOPIC_NOTIFICATION  = "notification.event";

    private final KafkaTemplate<String, String> kafka;
    private final ObjectMapper objectMapper;

    public FanOutPublisherService(KafkaTemplate<String, String> kafka, ObjectMapper objectMapper) {
        this.kafka        = kafka;
        this.objectMapper = objectMapper;
    }

    /**
     * Publishes a LIVESTREAM_STARTED_VIDEO event to {@code livestream.event}.
     * FeedFanoutConsumer.onLivestreamEvent picks this up and fans out to all followers,
     * applying the large-broadcaster batching policy for broadcasters with >10,000 followers.
     */
    public void publishStreamStarted(String streamKey, UUID userId, String displayName,
                                      String avatarUrl, String hlsManifestUrl, String rtspUrl,
                                      String thumbnailUrl) {
        LivestreamKafkaEvent event = new LivestreamKafkaEvent();
        event.setEventId(UUID.nameUUIDFromBytes(streamKey.getBytes()));
        event.setEventType("LIVESTREAM_STARTED_VIDEO");
        event.setActorId(userId);
        event.setActorDisplayName(displayName);
        event.setActorAvatarUrl(avatarUrl);
        event.setStreamId(UUID.nameUUIDFromBytes(streamKey.getBytes()));
        event.setLive(true);
        event.setHlsManifestUrl(hlsManifestUrl);
        event.setRtspUrl(rtspUrl);
        event.setStreamThumbnailUrl(thumbnailUrl);
        event.setViewerCount(0);
        Instant now = Instant.now();
        event.setStreamStartedAt(now);
        event.setPostedAt(now);

        sendEvent(TOPIC_LIVESTREAM, streamKey, event);

        // Also push a LIVESTREAM_STARTED notification to the broadcaster's followers
        publishNotification(userId, displayName,
            "LIVESTREAM_STARTED", hlsManifestUrl);
    }

    /**
     * Publishes a LIVESTREAM_ENDED event to {@code livestream.event}.
     */
    public void publishStreamEnded(String streamKey, UUID userId, String displayName) {
        LivestreamKafkaEvent event = new LivestreamKafkaEvent();
        event.setEventId(UUID.randomUUID());
        event.setEventType("LIVESTREAM_ENDED");
        event.setActorId(userId);
        event.setActorDisplayName(displayName);
        event.setStreamId(UUID.nameUUIDFromBytes(streamKey.getBytes()));
        event.setLive(false);
        event.setPostedAt(Instant.now());

        sendEvent(TOPIC_LIVESTREAM, streamKey, event);
    }

    /**
     * Publishes a LIVESTREAM_TERMINATED event (admin force-kill).
     */
    public void publishStreamTerminated(String streamKey, UUID userId, String displayName) {
        LivestreamKafkaEvent event = new LivestreamKafkaEvent();
        event.setEventId(UUID.randomUUID());
        event.setEventType("LIVESTREAM_ENDED"); // maps to LIVESTREAM_ENDED in FeedService
        event.setActorId(userId);
        event.setActorDisplayName(displayName);
        event.setStreamId(UUID.nameUUIDFromBytes(streamKey.getBytes()));
        event.setLive(false);
        event.setPostedAt(Instant.now());

        sendEvent(TOPIC_LIVESTREAM, streamKey, event);
        log.info("Published STREAM_TERMINATED for streamKey={}", streamKey);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void publishNotification(UUID userId, String broadcasterName,
                                      String type, String deepLinkUrl) {
        try {
            Map<String, Object> notif = Map.of(
                "recipientId",       userId.toString(),
                "type",              type,
                "title",             broadcasterName + " is live!",
                "body",              "Tap to watch the stream.",
                "deduplicationKey",  type + ":" + userId + ":" + Instant.now().getEpochSecond(),
                "data",              Map.of("deepLinkUrl", deepLinkUrl != null ? deepLinkUrl : ""),
                "occurredAt",        Instant.now().toString()
            );
            kafka.send(TOPIC_NOTIFICATION, objectMapper.writeValueAsString(notif));
        } catch (JsonProcessingException e) {
            log.warn("Failed to publish notification for userId={}: {}", userId, e.getMessage());
        }
    }

    private void sendEvent(String topic, String key, LivestreamKafkaEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            kafka.send(topic, key, payload);
            log.info("Published {} to topic={}", event.getEventType(), topic);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize {} event for key={}: {}", event.getEventType(), key, e.getMessage());
        }
    }
}
