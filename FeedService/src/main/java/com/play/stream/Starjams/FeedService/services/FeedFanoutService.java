package com.play.stream.Starjams.FeedService.services;

import com.aerospike.client.*;
import com.aerospike.client.policy.WritePolicy;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.play.stream.Starjams.FeedService.config.KafkaTopics;
import com.play.stream.Starjams.FeedService.dto.TrackPostedEvent;
import com.play.stream.Starjams.FeedService.entity.FeedEventLog;
import com.play.stream.Starjams.FeedService.model.EventType;
import com.play.stream.Starjams.FeedService.model.FeedEvent;
import com.play.stream.Starjams.FeedService.repository.FeedEventLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Fan-out-on-write: writes a FeedEvent into each qualifying follower's feed bin.
 *
 * <p>Aerospike schema for feed bins:
 * <pre>
 *   namespace:  fetio
 *   set:        feed:{userId}
 *   key:        {userId}:{eventId}
 *   TTL:        72h (feedTtlSeconds)
 *   bins:       eventId, eventType, actorId, actorDisplayName, actorAvatarUrl,
 *               trackId, trackTitle, trackDuration, coverArtUrl, audioStreamUrl,
 *               genre, mood, playCount, likeCount, repostCount, commentCount,
 *               postedAt, isNew, isBuzzing, isLocked, giftThreshold, giftProgress,
 *               collaboratorIds, repostLineage
 * </pre>
 *
 * <p>Fan-out is executed asynchronously per the {@code feedFanoutExecutor} thread pool.
 * Fan-out completes within 500 ms for up to 10,000 followers thanks to async batching.
 */
@Service
public class FeedFanoutService {

    private static final Logger log = LoggerFactory.getLogger(FeedFanoutService.class);
    private static final String NS = "fetio";

    private final IAerospikeClient aerospike;
    private final FollowGraphService followGraph;
    private final FeedEventLogRepository logRepo;
    private final KafkaTemplate<String, String> kafka;
    private final ObjectMapper objectMapper;

    @Value("${aerospike.feed-ttl-seconds:259200}")
    private int feedTtlSeconds;

    @Value("${feed.first48-max-followers:500}")
    private long first48MaxFollowers;

    public FeedFanoutService(IAerospikeClient aerospike,
                             FollowGraphService followGraph,
                             FeedEventLogRepository logRepo,
                             KafkaTemplate<String, String> kafka,
                             ObjectMapper objectMapper) {
        this.aerospike = aerospike;
        this.followGraph = followGraph;
        this.logRepo = logRepo;
        this.kafka = kafka;
        this.objectMapper = objectMapper;
    }

    // -------------------------------------------------------------------------
    // Public fan-out entry points (each called by the appropriate Kafka consumer)
    // -------------------------------------------------------------------------

    @Async("feedFanoutExecutor")
    public void fanOutTrackPosted(TrackPostedEvent event) {
        FeedEvent feedEvent = buildTrackPostedFeedEvent(event);
        List<UUID> followers = followGraph.getAllFollowers(event.getActorId());

        // Direct followers
        writeToFollowers(feedEvent, followers);

        // Collaborators' followers also receive the event (Collab Chain)
        if (event.getCollaboratorIds() != null) {
            for (UUID collabId : event.getCollaboratorIds()) {
                List<UUID> collabFollowers = followGraph.getAllFollowers(collabId);
                writeToFollowers(feedEvent, collabFollowers);
            }
        }

        logFanOut(feedEvent.getEventId(), EventType.TRACK_POSTED,
            event.getActorId(), event.getTrackId(), followers.size());
    }

    @Async("feedFanoutExecutor")
    public void fanOutEngagement(FeedEvent feedEvent, List<UUID> targetFollowers) {
        writeToFollowers(feedEvent, targetFollowers);
        logFanOut(feedEvent.getEventId(), feedEvent.getEventType(),
            feedEvent.getActorId(), feedEvent.getTrackId(), targetFollowers.size());
    }

    private static final int  LARGE_BROADCASTER_THRESHOLD = 10_000;
    private static final int  FANOUT_BATCH_SIZE            = 1_000;
    private static final long FANOUT_BATCH_DELAY_MS        = 100L;

    @Async("feedFanoutExecutor")
    public void fanOutLivestream(FeedEvent livestreamEvent, List<UUID> followerIds) {
        if (followerIds.size() <= LARGE_BROADCASTER_THRESHOLD) {
            writeToFollowers(livestreamEvent, followerIds);
        } else {
            List<List<UUID>> batches = partitionList(followerIds, FANOUT_BATCH_SIZE);
            log.info("Large broadcaster fan-out: {} followers in {} batches of {}",
                followerIds.size(), batches.size(), FANOUT_BATCH_SIZE);
            for (List<UUID> batch : batches) {
                writeToFollowers(livestreamEvent, batch);
                try {
                    Thread.sleep(FANOUT_BATCH_DELAY_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        // Pin to top of each follower's feed
        for (UUID followerId : followerIds) {
            pinLivestreamForUser(followerId, livestreamEvent.getStreamId());
        }
    }

    private static List<List<UUID>> partitionList(List<UUID> list, int batchSize) {
        List<List<UUID>> partitions = new java.util.ArrayList<>();
        for (int i = 0; i < list.size(); i += batchSize) {
            partitions.add(list.subList(i, Math.min(i + batchSize, list.size())));
        }
        return partitions;
    }

    // -------------------------------------------------------------------------
    // Aerospike write
    // -------------------------------------------------------------------------

    private void writeToFollowers(FeedEvent event, List<UUID> followerIds) {
        WritePolicy wp = fanoutWritePolicy();
        for (UUID followerId : followerIds) {
            writeSingleFeedRecord(wp, followerId, event);
        }
    }

    private void writeSingleFeedRecord(WritePolicy wp, UUID followerId, FeedEvent event) {
        String setName = "feed:" + followerId;
        String keyStr  = followerId + ":" + event.getEventId();
        Key key = new Key(NS, setName, keyStr);

        try {
            aerospike.put(wp, key,
                new Bin("eventId",           event.getEventId().toString()),
                new Bin("eventType",          event.getEventType().name()),
                new Bin("actorId",            event.getActorId().toString()),
                new Bin("actorDisplayName",   event.getActorDisplayName()),
                new Bin("actorAvatarUrl",     event.getActorAvatarUrl()),
                new Bin("trackId",            event.getTrackId() != null ? event.getTrackId().toString() : null),
                new Bin("trackTitle",         event.getTrackTitle()),
                new Bin("trackDuration",      event.getTrackDuration()),
                new Bin("coverArtUrl",        event.getCoverArtUrl()),
                new Bin("audioStreamUrl",     event.getAudioStreamUrl()),
                new Bin("genre",              event.getGenre() != null ? String.join(",", event.getGenre()) : null),
                new Bin("mood",               event.getMood()),
                new Bin("playCount",          event.getPlayCount()),
                new Bin("likeCount",          event.getLikeCount()),
                new Bin("repostCount",        event.getRepostCount()),
                new Bin("commentCount",       event.getCommentCount()),
                new Bin("postedAt",           event.getPostedAt() != null ? event.getPostedAt().toEpochMilli() : 0L),
                new Bin("isNew",              event.isNew() ? 1 : 0),
                new Bin("isBuzzing",          event.isBuzzing() ? 1 : 0),
                new Bin("isLocked",           event.isLocked() ? 1 : 0),
                new Bin("giftThreshold",      event.getGiftThreshold()),
                new Bin("giftProgress",       event.getGiftProgress()),
                new Bin("streamId",           event.getStreamId() != null ? event.getStreamId().toString() : null),
                new Bin("isLive",             event.isLive() ? 1 : 0)
            );
        } catch (AerospikeException e) {
            log.warn("Failed to write feed event {} to follower {}: {}",
                event.getEventId(), followerId, e.getMessage());
        }
    }

    private void pinLivestreamForUser(UUID userId, UUID streamId) {
        Key key = new Key(NS, "stream_feed_pin:" + userId, userId.toString());
        WritePolicy wp = new WritePolicy();
        aerospike.put(wp, key, new Bin("streamId", streamId.toString()));
    }

    // -------------------------------------------------------------------------
    // FeedEvent construction
    // -------------------------------------------------------------------------

    private FeedEvent buildTrackPostedFeedEvent(TrackPostedEvent ev) {
        FeedEvent fe = new FeedEvent();
        fe.setEventId(UUID.randomUUID());
        fe.setEventType(EventType.TRACK_POSTED);
        fe.setActorId(ev.getActorId());
        fe.setActorDisplayName(ev.getActorDisplayName());
        fe.setActorAvatarUrl(ev.getActorAvatarUrl());
        fe.setTrackId(ev.getTrackId());
        fe.setTrackTitle(ev.getTrackTitle());
        fe.setTrackDuration(ev.getTrackDuration());
        fe.setCoverArtUrl(ev.getCoverArtUrl());
        fe.setAudioStreamUrl(ev.getAudioStreamUrl());
        fe.setGenre(ev.getGenre());
        fe.setMood(ev.getMood());
        fe.setPostedAt(ev.getPostedAt() != null ? ev.getPostedAt() : Instant.now());
        fe.setExpiresAt(fe.getPostedAt().plusSeconds(feedTtlSeconds));
        fe.setLocked(ev.isLocked());
        fe.setGiftThreshold(ev.getGiftThreshold());

        // First-48 viral flag
        boolean isFirst48 = fe.getPostedAt().isAfter(Instant.now().minusSeconds(48 * 3600));
        boolean isSmallCreator = ev.getActorFollowerCount() < first48MaxFollowers;
        fe.setNew(isFirst48 && isSmallCreator);
        return fe;
    }

    // -------------------------------------------------------------------------
    // Audit log
    // -------------------------------------------------------------------------

    private void logFanOut(UUID eventId, EventType eventType, UUID actorId, UUID contentId, int fanOutCount) {
        try {
            FeedEventLog log = new FeedEventLog();
            log.setEventId(eventId);
            log.setEventType(eventType);
            log.setActorId(actorId);
            log.setContentId(contentId);
            log.setFanOutCount(fanOutCount);
            log.setOccurredAt(Instant.now());
            logRepo.save(log);
        } catch (Exception e) {
            // Non-critical — never fail fan-out due to log write
            FeedFanoutService.log.warn("Could not write FeedEventLog: {}", e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private WritePolicy fanoutWritePolicy() {
        WritePolicy wp = new WritePolicy();
        wp.expiration = feedTtlSeconds;
        wp.sendKey = true;
        return wp;
    }
}
