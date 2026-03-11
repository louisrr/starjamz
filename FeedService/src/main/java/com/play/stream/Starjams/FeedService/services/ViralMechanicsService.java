package com.play.stream.Starjams.FeedService.services;

import com.aerospike.client.*;
import com.aerospike.client.cdt.ListOperation;
import com.aerospike.client.cdt.MapOperation;
import com.aerospike.client.cdt.MapOrder;
import com.aerospike.client.cdt.MapPolicy;
import com.aerospike.client.cdt.MapWriteMode;
import com.aerospike.client.policy.WritePolicy;
import com.play.stream.Starjams.FeedService.config.KafkaTopics;
import com.play.stream.Starjams.FeedService.model.EventType;
import com.play.stream.Starjams.FeedService.model.FeedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

/**
 * Implements the viral mechanics layer:
 *
 * <ul>
 *   <li><b>Repost with Commentary ("Flip It")</b> — fan out reposts with lineage tracking.
 *   <li><b>First 48 viral window</b> — isNew flag + small-creator 2.5× boost (applied in ranking).
 *   <li><b>Buzzing detection</b> — ≥100 plays in 6 h → isBuzzing=true + discovery fan-out.
 *   <li><b>Gift-to-Unlock</b> — progress tracking; auto-unlock when threshold met.
 *   <li><b>Play Streak</b> — 3+ days in a row on same artist → notification event.
 * </ul>
 */
@Service
public class ViralMechanicsService {

    private static final Logger log = LoggerFactory.getLogger(ViralMechanicsService.class);
    private static final String NS = "fetio";

    private static final MapPolicy MAP_POLICY =
        new MapPolicy(MapOrder.KEY_ORDERED, MapWriteMode.UPDATE);

    @Value("${feed.buzzing-play-threshold:100}")
    private long buzzingPlayThreshold;

    @Value("${feed.buzzing-window-hours:6}")
    private long buzzingWindowHours;

    @Value("${feed.streak-days:3}")
    private int streakDays;

    private final IAerospikeClient aerospike;
    private final FeedFanoutService fanout;
    private final FollowGraphService followGraph;
    private final KafkaTemplate<String, String> kafka;

    public ViralMechanicsService(IAerospikeClient aerospike,
                                  FeedFanoutService fanout,
                                  FollowGraphService followGraph,
                                  KafkaTemplate<String, String> kafka) {
        this.aerospike = aerospike;
        this.fanout    = fanout;
        this.followGraph = followGraph;
        this.kafka     = kafka;
    }

    // -------------------------------------------------------------------------
    // Repost with Commentary ("Flip It")
    // -------------------------------------------------------------------------

    @Async("feedFanoutExecutor")
    public void handleRepost(UUID reposterActorId,
                             String reposterDisplayName,
                             String reposterAvatarUrl,
                             FeedEvent originalEvent,
                             String commentText,
                             String commentAudioUrl) {
        FeedEvent repost = new FeedEvent();
        repost.setEventId(UUID.randomUUID());
        repost.setEventType(EventType.TRACK_REPOSTED);
        repost.setActorId(reposterActorId);
        repost.setActorDisplayName(reposterDisplayName);
        repost.setActorAvatarUrl(reposterAvatarUrl);

        // Preserve original track data
        repost.setTrackId(originalEvent.getTrackId());
        repost.setTrackTitle(originalEvent.getTrackTitle());
        repost.setTrackDuration(originalEvent.getTrackDuration());
        repost.setCoverArtUrl(originalEvent.getCoverArtUrl());
        repost.setAudioStreamUrl(originalEvent.getAudioStreamUrl());
        repost.setGenre(originalEvent.getGenre());
        repost.setMood(originalEvent.getMood());

        // Credit original artist
        repost.setOriginalActorId(originalEvent.getActorId());
        repost.setOriginalActorDisplayName(originalEvent.getActorDisplayName());
        repost.setRepostCommentText(commentText);
        repost.setRepostCommentAudioUrl(commentAudioUrl);

        // Build/extend lineage chain
        List<UUID> lineage = new ArrayList<>();
        if (originalEvent.getRepostLineage() != null) {
            lineage.addAll(originalEvent.getRepostLineage());
        }
        lineage.add(originalEvent.getActorId());
        repost.setRepostLineage(lineage);

        repost.setPostedAt(Instant.now());
        repost.setNew(false); // reposts don't get First-48 boost
        repost.setBuzzing(originalEvent.isBuzzing());

        // Fan out to reposter's followers
        List<UUID> reposterFollowers = followGraph.getAllFollowers(reposterActorId);
        fanout.fanOutEngagement(repost, reposterFollowers);
    }

    // -------------------------------------------------------------------------
    // Buzzing detection (called after every play increment)
    // -------------------------------------------------------------------------

    /**
     * Checks if a track has crossed the buzzing threshold (100+ plays in 6h).
     * If newly buzzing, marks it in Aerospike and fans out to a discovery set.
     */
    public void checkBuzzingThreshold(String trackId) {
        Key statsKey = new Key(NS, "track_stats:" + trackId, trackId);
        Record stats = aerospike.get(null, statsKey, "playCount6h", "isBuzzing", "postedAt");
        if (stats == null) return;

        if (stats.getLong("isBuzzing") == 1) return; // already marked

        long plays6h = stats.getLong("playCount6h");
        if (plays6h >= buzzingPlayThreshold) {
            markBuzzing(trackId, statsKey);
        }
    }

    private void markBuzzing(String trackId, Key statsKey) {
        WritePolicy wp = new WritePolicy();
        aerospike.put(wp, statsKey, new Bin("isBuzzing", 1L));
        log.info("Track {} is now BUZZING — scheduling discovery fan-out", trackId);
        // Discovery fan-out would fan event to a wider discovery audience
        // Implementation delegates to a discovery feed expansion (out of scope for MVP)
    }

    // -------------------------------------------------------------------------
    // Gift-to-Unlock
    // -------------------------------------------------------------------------

    /**
     * Records a gift against a locked track and checks if the threshold is met.
     * Atomic increment via Aerospike operate() — no race conditions.
     *
     * @return true if this gift triggered the unlock
     */
    public boolean recordGift(UUID trackId) {
        Key key = new Key(NS, "track_stats:" + trackId.toString(), trackId.toString());
        WritePolicy wp = new WritePolicy();
        Record after = aerospike.operate(wp, key,
            Operation.add(new Bin("giftProgress", 1L)),
            Operation.get("giftProgress"),
            Operation.get("giftThreshold"));

        if (after == null) return false;

        long progress  = after.getLong("giftProgress");
        long threshold = after.getLong("giftThreshold");

        if (progress >= threshold && threshold > 0) {
            unlockTrack(trackId, key);
            return true;
        }
        return false;
    }

    private void unlockTrack(UUID trackId, Key key) {
        WritePolicy wp = new WritePolicy();
        aerospike.put(wp, key, new Bin("isLocked", 0L));
        log.info("Track {} unlocked via gifting threshold", trackId);
        // Publish notification event so NotificationService notifies participants
        kafka.send(KafkaTopics.NOTIFICATION_EVENT,
            "{\"type\":\"GIFT_UNLOCK\",\"trackId\":\"" + trackId + "\"}");
    }

    // -------------------------------------------------------------------------
    // Play streak tracking (drives follow nudge notifications)
    // -------------------------------------------------------------------------

    /**
     * Records that {@code userId} played an artist's track today.
     * If the streak reaches {@code streakDays}, emits a notification event.
     */
    public void recordPlay(UUID userId, UUID artistId) {
        String setName = "play_streak:" + userId;
        Key key = new Key(NS, setName, artistId.toString());
        WritePolicy wp = new WritePolicy();
        long todayEpochDay = Instant.now().getEpochSecond() / 86400;

        Record rec = aerospike.get(null, key, "lastPlayDay", "streakCount");
        if (rec == null) {
            aerospike.put(wp, key,
                new Bin("lastPlayDay",  todayEpochDay),
                new Bin("streakCount",  1L),
                new Bin("artistId",     artistId.toString()));
            return;
        }

        long lastPlayDay  = rec.getLong("lastPlayDay");
        long streakCount  = rec.getLong("streakCount");

        if (lastPlayDay == todayEpochDay) return; // already counted today

        if (todayEpochDay - lastPlayDay == 1) {
            // Consecutive day
            long newStreak = streakCount + 1;
            aerospike.put(wp, key,
                new Bin("lastPlayDay", todayEpochDay),
                new Bin("streakCount", newStreak));
            if (newStreak >= streakDays) {
                kafka.send(KafkaTopics.NOTIFICATION_EVENT,
                    "{\"type\":\"PLAY_STREAK\",\"userId\":\"" + userId
                        + "\",\"artistId\":\"" + artistId
                        + "\",\"streakDays\":" + newStreak + "}");
            }
        } else {
            // Streak broken — reset
            aerospike.put(wp, key,
                new Bin("lastPlayDay", todayEpochDay),
                new Bin("streakCount", 1L));
        }
    }
}
