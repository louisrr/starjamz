package com.play.stream.Starjams.FeedService.services;

import com.aerospike.client.*;
import com.aerospike.client.cdt.MapOperation;
import com.aerospike.client.cdt.MapOrder;
import com.aerospike.client.cdt.MapPolicy;
import com.aerospike.client.cdt.MapReturnType;
import com.aerospike.client.cdt.MapWriteMode;
import com.aerospike.client.policy.WritePolicy;
import com.play.stream.Starjams.FeedService.entity.Follow;
import com.play.stream.Starjams.FeedService.repository.FollowRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

/**
 * Manages the asymmetric follow graph (Twitter-style).
 *
 * <p>Hot path: Aerospike CDT Map — O(1) follow/unfollow, O(1) membership check.
 * <p>Durable path: PostgreSQL via {@link FollowRepository} — system of record,
 *    analytics queries, follower counts for privacy-threshold checks.
 *
 * <p>Aerospike schema:
 * <pre>
 *   namespace: fetio
 *   set: follows:{userId}   key=userId  bin "followees" → Map<String,Long> (targetId → epochMs)
 *   set: followers:{userId} key=userId  bin "followers_map" → Map<String,Long> (followerId → epochMs)
 * </pre>
 */
@Service
public class FollowGraphService {

    private static final String NS            = "fetio";
    private static final String BIN_FOLLOWEES = "followees";
    private static final String BIN_FOLLOWERS = "followers_map";

    private static final MapPolicy MAP_POLICY =
        new MapPolicy(MapOrder.KEY_ORDERED, MapWriteMode.UPDATE);

    private final IAerospikeClient aerospike;
    private final FollowRepository followRepo;

    @Value("${aerospike.namespace:fetio}")
    private String namespace;

    public FollowGraphService(IAerospikeClient aerospike, FollowRepository followRepo) {
        this.aerospike = aerospike;
        this.followRepo = followRepo;
    }

    // -------------------------------------------------------------------------
    // Follow / Unfollow
    // -------------------------------------------------------------------------

    @Transactional
    public void follow(UUID followerId, UUID targetId) {
        if (followerId.equals(targetId)) {
            throw new IllegalArgumentException("Users cannot follow themselves");
        }

        long now = Instant.now().toEpochMilli();

        // Aerospike: add to followees map for follower
        Key followeeKey = new Key(NS, "follows:" + followerId, followerId.toString());
        aerospike.operate(defaultWritePolicy(), followeeKey,
            MapOperation.put(MAP_POLICY, BIN_FOLLOWEES,
                Value.get(targetId.toString()), Value.get(now)));

        // Aerospike: add to followers map for target
        Key followerKey = new Key(NS, "followers:" + targetId, targetId.toString());
        aerospike.operate(defaultWritePolicy(), followerKey,
            MapOperation.put(MAP_POLICY, BIN_FOLLOWERS,
                Value.get(followerId.toString()), Value.get(now)));

        // PostgreSQL: durable record (upsert — ignore duplicates)
        if (followRepo.findByFollowerIdAndFolloweeId(followerId, targetId).isEmpty()) {
            followRepo.save(new Follow(followerId, targetId));
        }
    }

    @Transactional
    public void unfollow(UUID followerId, UUID targetId) {
        // Aerospike: remove from followees
        Key followeeKey = new Key(NS, "follows:" + followerId, followerId.toString());
        aerospike.operate(defaultWritePolicy(), followeeKey,
            MapOperation.removeByKey(BIN_FOLLOWEES,
                Value.get(targetId.toString()), MapReturnType.NONE));

        // Aerospike: remove from followers
        Key followerKey = new Key(NS, "followers:" + targetId, targetId.toString());
        aerospike.operate(defaultWritePolicy(), followerKey,
            MapOperation.removeByKey(BIN_FOLLOWERS,
                Value.get(followerId.toString()), MapReturnType.NONE));

        // PostgreSQL: hard delete
        followRepo.deleteByFollowerIdAndFolloweeId(followerId, targetId);
    }

    // -------------------------------------------------------------------------
    // Read — following list (who this user follows)
    // -------------------------------------------------------------------------

    public List<UUID> getFollowingPage(UUID userId, int offset, int limit) {
        Key key = new Key(NS, "follows:" + userId, userId.toString());
        Record rec = aerospike.operate(null, key,
            MapOperation.getByIndexRange(BIN_FOLLOWEES, offset, limit, MapReturnType.KEY));
        if (rec == null) return Collections.emptyList();
        List<?> keys = (List<?>) rec.getValue(BIN_FOLLOWEES);
        return toUUIDs(keys);
    }

    public long getFollowingCount(UUID userId) {
        return followRepo.countByFollowerId(userId);
    }

    // -------------------------------------------------------------------------
    // Read — followers list (who follows this user)
    // -------------------------------------------------------------------------

    public List<UUID> getFollowersPage(UUID userId, int offset, int limit) {
        Key key = new Key(NS, "followers:" + userId, userId.toString());
        Record rec = aerospike.operate(null, key,
            MapOperation.getByIndexRange(BIN_FOLLOWERS, offset, limit, MapReturnType.KEY));
        if (rec == null) return Collections.emptyList();
        List<?> keys = (List<?>) rec.getValue(BIN_FOLLOWERS);
        return toUUIDs(keys);
    }

    public long getFollowerCount(UUID userId) {
        return followRepo.countByFolloweeId(userId);
    }

    /**
     * Returns all follower IDs for a user. Used by fan-out — streams results
     * in pages of 500 to avoid holding a massive list in memory.
     */
    public List<UUID> getAllFollowers(UUID userId) {
        Page<Follow> page = followRepo.findByFolloweeId(userId, PageRequest.of(0, 500));
        List<UUID> result = new ArrayList<>();
        page.forEach(f -> result.add(f.getFollowerId()));
        int totalPages = page.getTotalPages();
        for (int p = 1; p < totalPages; p++) {
            followRepo.findByFolloweeId(userId, PageRequest.of(p, 500))
                .forEach(f -> result.add(f.getFollowerId()));
        }
        return result;
    }

    // -------------------------------------------------------------------------
    // Membership check
    // -------------------------------------------------------------------------

    public boolean isFollowing(UUID followerId, UUID targetId) {
        Key key = new Key(NS, "follows:" + followerId, followerId.toString());
        Record rec = aerospike.operate(null, key,
            MapOperation.getByKey(BIN_FOLLOWEES,
                Value.get(targetId.toString()), MapReturnType.VALUE));
        if (rec == null) return false;
        return rec.getValue(BIN_FOLLOWEES) != null;
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private WritePolicy defaultWritePolicy() {
        WritePolicy wp = new WritePolicy();
        wp.sendKey = true;
        return wp;
    }

    @SuppressWarnings("unchecked")
    private List<UUID> toUUIDs(List<?> raw) {
        if (raw == null) return Collections.emptyList();
        List<UUID> result = new ArrayList<>(raw.size());
        for (Object o : raw) {
            result.add(UUID.fromString(o.toString()));
        }
        return result;
    }
}
