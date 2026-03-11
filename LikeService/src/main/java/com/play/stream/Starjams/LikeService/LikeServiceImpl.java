package com.play.stream.Starjams.LikeService;

import com.aerospike.client.*;
import com.aerospike.client.cdt.ListOperation;
import com.aerospike.client.cdt.ListOrder;
import com.aerospike.client.cdt.ListPolicy;
import com.aerospike.client.cdt.ListReturnType;
import com.aerospike.client.cdt.MapOperation;
import com.aerospike.client.cdt.MapOrder;
import com.aerospike.client.cdt.MapPolicy;
import com.aerospike.client.cdt.MapReturnType;
import com.aerospike.client.cdt.MapWriteMode;
import com.aerospike.client.policy.WritePolicy;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.play.stream.Starjams.LikeService.config.KafkaTopics;
import com.play.stream.Starjams.LikeService.dto.LikeCountResponse;
import com.play.stream.Starjams.LikeService.dto.LikeStatusResponse;
import com.play.stream.Starjams.LikeService.dto.LikedContentResponse;
import com.play.stream.Starjams.LikeService.dto.PagedLikersResponse;
import com.play.stream.Starjams.LikeService.dto.UserLikesPageResponse;
import com.play.stream.Starjams.LikeService.entity.Like;
import com.play.stream.Starjams.LikeService.model.ContentType;
import com.play.stream.Starjams.LikeService.repository.LikeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Core LikeService implementation.
 *
 * <p>Write path:
 * <ol>
 *   <li>Idempotency check via Aerospike {@code user_likes:{userId}} Map CDT</li>
 *   <li>Atomic increment / decrement of {@code like_count:{type}:{id}} integer bin</li>
 *   <li>Prepend / remove from {@code liked_by:{type}:{id}} list bin (capped at 1000)</li>
 *   <li>Update {@code user_likes:{userId}} membership map</li>
 *   <li>Async PostgreSQL write via Kafka {@code db.write}</li>
 *   <li>Publish {@code content.liked} / {@code content.unliked} to downstream consumers</li>
 * </ol>
 *
 * <p>Read path:
 * <ul>
 *   <li>Like count and status: Aerospike only (p99 &lt; 10 ms target)</li>
 *   <li>Recent likers: Aerospike list, fallback to PostgreSQL for full history</li>
 *   <li>User's liked content: PostgreSQL with cursor pagination</li>
 * </ul>
 */
@Service
public class LikeServiceImpl implements LikeService {

    private static final Logger log = LoggerFactory.getLogger(LikeServiceImpl.class);

    // Aerospike namespace
    private static final String NS = "fetio";

    // Bin names
    private static final String BIN_TOTAL_LIKES  = "totalLikes";
    private static final String BIN_RECENT_LIKERS = "recentLikers";
    private static final String BIN_LIKED_ITEMS   = "likedItems";

    private static final MapPolicy MAP_POLICY =
            new MapPolicy(MapOrder.KEY_ORDERED, MapWriteMode.UPDATE);

    private static final ListPolicy LIST_POLICY =
            new ListPolicy(ListOrder.UNORDERED, 0);

    @Value("${aerospike.namespace:fetio}")
    private String namespace;

    @Value("${like.liked-by-max-size:1000}")
    private int likedByMaxSize;

    private final IAerospikeClient aerospike;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final LikeRepository likeRepository;
    private final ObjectMapper objectMapper;

    public LikeServiceImpl(IAerospikeClient aerospike,
                           KafkaTemplate<String, String> kafkaTemplate,
                           LikeRepository likeRepository,
                           ObjectMapper objectMapper) {
        this.aerospike      = aerospike;
        this.kafkaTemplate  = kafkaTemplate;
        this.likeRepository = likeRepository;
        this.objectMapper   = objectMapper;
    }

    // -------------------------------------------------------------------------
    // Write path: LIKE
    // -------------------------------------------------------------------------

    @Override
    public void like(UUID userId, UUID contentId, ContentType contentType) {
        String membershipToken = membershipToken(contentType, contentId);

        // 1. Idempotency check — reject silently if already liked
        if (isLikedInAerospike(userId, membershipToken)) {
            log.debug("Duplicate like ignored: user={} content={}/{}", userId, contentType, contentId);
            return;
        }

        WritePolicy wp = writePolicy();

        // 2. Atomic increment like count
        Key countKey = likeCountKey(contentType, contentId);
        aerospike.operate(wp, countKey, Operation.add(new Bin(BIN_TOTAL_LIKES, 1)));

        // 3. Prepend userId to liked_by list, trim to max size
        Key likedByKey = likedByKey(contentType, contentId);
        aerospike.operate(wp, likedByKey,
                ListOperation.insert(LIST_POLICY, BIN_RECENT_LIKERS, 0, Value.get(userId.toString())),
                ListOperation.trim(BIN_RECENT_LIKERS, 0, likedByMaxSize - 1));

        // 4. Add to user_likes membership map
        Key userLikesKey = userLikesKey(userId);
        aerospike.operate(wp, userLikesKey,
                MapOperation.put(MAP_POLICY, BIN_LIKED_ITEMS,
                        Value.get(membershipToken), Value.get(Instant.now().toEpochMilli())));

        // 5. Async PostgreSQL insert via Kafka db.write
        publishDbWrite("INSERT", userId, contentId, contentType);

        // 6. Publish content.liked event for FeedService / NotificationService
        publishLikeEvent(KafkaTopics.CONTENT_LIKED, userId, contentId, contentType);
    }

    // -------------------------------------------------------------------------
    // Write path: UNLIKE
    // -------------------------------------------------------------------------

    @Override
    public void unlike(UUID userId, UUID contentId, ContentType contentType) {
        String membershipToken = membershipToken(contentType, contentId);

        // 1. Idempotency check — reject silently if not liked
        if (!isLikedInAerospike(userId, membershipToken)) {
            log.debug("Unlike ignored (not liked): user={} content={}/{}", userId, contentType, contentId);
            return;
        }

        WritePolicy wp = writePolicy();

        // 2. Atomic decrement (never below 0)
        Key countKey = likeCountKey(contentType, contentId);
        Record countRec = aerospike.get(null, countKey, BIN_TOTAL_LIKES);
        if (countRec != null) {
            long current = countRec.getLong(BIN_TOTAL_LIKES);
            if (current > 0) {
                aerospike.operate(wp, countKey, Operation.add(new Bin(BIN_TOTAL_LIKES, -1)));
            }
        }

        // 3. Remove userId from liked_by list
        Key likedByKey = likedByKey(contentType, contentId);
        aerospike.operate(wp, likedByKey,
                ListOperation.removeByValue(BIN_RECENT_LIKERS,
                        Value.get(userId.toString()), ListReturnType.NONE));

        // 4. Remove from user_likes membership map
        Key userLikesKey = userLikesKey(userId);
        aerospike.operate(wp, userLikesKey,
                MapOperation.removeByKey(BIN_LIKED_ITEMS,
                        Value.get(membershipToken), MapReturnType.NONE));

        // 5. Async PostgreSQL delete via Kafka db.write
        publishDbWrite("DELETE", userId, contentId, contentType);

        // 6. Publish content.unliked
        publishLikeEvent(KafkaTopics.CONTENT_UNLIKED, userId, contentId, contentType);
    }

    // -------------------------------------------------------------------------
    // Read path: COUNT
    // -------------------------------------------------------------------------

    @Override
    public LikeCountResponse getLikeCount(UUID contentId, ContentType contentType) {
        Key countKey = likeCountKey(contentType, contentId);
        Record rec = aerospike.get(null, countKey, BIN_TOTAL_LIKES);
        long total = (rec != null) ? rec.getLong(BIN_TOTAL_LIKES) : 0L;
        return new LikeCountResponse(contentId, contentType, total);
    }

    // -------------------------------------------------------------------------
    // Read path: STATUS (liked? + count) — both from Aerospike
    // -------------------------------------------------------------------------

    @Override
    public LikeStatusResponse getLikeStatus(UUID userId, UUID contentId, ContentType contentType) {
        String membershipToken = membershipToken(contentType, contentId);
        boolean liked = isLikedInAerospike(userId, membershipToken);

        Key countKey = likeCountKey(contentType, contentId);
        Record countRec = aerospike.get(null, countKey, BIN_TOTAL_LIKES);
        long total = (countRec != null) ? countRec.getLong(BIN_TOTAL_LIKES) : 0L;

        return new LikeStatusResponse(liked, total);
    }

    // -------------------------------------------------------------------------
    // Read path: RECENT LIKERS
    // -------------------------------------------------------------------------

    @Override
    public PagedLikersResponse getRecentLikers(UUID contentId, ContentType contentType,
                                               int limit, String cursor) {
        // Recent likers are served from Aerospike list
        Key likedByKey = likedByKey(contentType, contentId);
        int offset = (cursor != null && !cursor.isBlank()) ? decodeCursorOffset(cursor) : 0;

        Record rec = aerospike.operate(null, likedByKey,
                ListOperation.getByIndexRange(BIN_RECENT_LIKERS, offset, limit + 1, ListReturnType.VALUE));

        if (rec == null) {
            // Aerospike miss — fall back to PostgreSQL
            return getRecentLikersFromPostgres(contentId, contentType, limit, offset);
        }

        @SuppressWarnings("unchecked")
        List<Object> raw = (List<Object>) rec.getValue(BIN_RECENT_LIKERS);
        if (raw == null || raw.isEmpty()) {
            return new PagedLikersResponse(Collections.emptyList(), null, false);
        }

        boolean hasMore = raw.size() > limit;
        List<String> page = raw.stream()
                .limit(limit)
                .map(Object::toString)
                .collect(Collectors.toList());

        String nextCursor = hasMore ? encodeCursorOffset(offset + limit) : null;
        return new PagedLikersResponse(page, nextCursor, hasMore);
    }

    // -------------------------------------------------------------------------
    // Read path: USER LIKES (PostgreSQL, cold path)
    // -------------------------------------------------------------------------

    @Override
    public UserLikesPageResponse getUserLikes(UUID userId, ContentType contentType,
                                              int limit, String cursor) {
        PageRequest pageable = PageRequest.of(0, limit);
        Slice<Like> slice;

        if (cursor != null && !cursor.isBlank()) {
            Instant cursorInstant = decodeCursorInstant(cursor);
            slice = likeRepository.findByUserIdAndContentTypeBeforeCursor(
                    userId, contentType, cursorInstant, pageable);
        } else {
            slice = likeRepository.findByUserIdAndContentTypeFirstPage(
                    userId, contentType, pageable);
        }

        List<LikedContentResponse> items = slice.getContent().stream()
                .map(l -> new LikedContentResponse(l.getLikeId(), l.getContentId(),
                        l.getContentType(), l.getCreatedAt()))
                .collect(Collectors.toList());

        String nextCursor = null;
        if (slice.hasNext() && !items.isEmpty()) {
            nextCursor = encodeCursorInstant(items.get(items.size() - 1).likedAt());
        }

        return new UserLikesPageResponse(items, nextCursor, slice.hasNext());
    }

    // -------------------------------------------------------------------------
    // Aerospike helpers
    // -------------------------------------------------------------------------

    private boolean isLikedInAerospike(UUID userId, String membershipToken) {
        Key userLikesKey = userLikesKey(userId);
        Record rec = aerospike.operate(null, userLikesKey,
                MapOperation.getByKey(BIN_LIKED_ITEMS,
                        Value.get(membershipToken), MapReturnType.VALUE));
        return rec != null && rec.getValue(BIN_LIKED_ITEMS) != null;
    }

    private Key likeCountKey(ContentType contentType, UUID contentId) {
        return new Key(NS, "like_count:" + contentType + ":" + contentId, "count");
    }

    private Key likedByKey(ContentType contentType, UUID contentId) {
        return new Key(NS, "liked_by:" + contentType + ":" + contentId, "likers");
    }

    private Key userLikesKey(UUID userId) {
        return new Key(NS, "user_likes:" + userId, userId.toString());
    }

    private String membershipToken(ContentType contentType, UUID contentId) {
        return contentType.name() + ":" + contentId;
    }

    private WritePolicy writePolicy() {
        WritePolicy wp = new WritePolicy();
        wp.sendKey = true;
        return wp;
    }

    // -------------------------------------------------------------------------
    // Kafka helpers
    // -------------------------------------------------------------------------

    private void publishLikeEvent(String topic, UUID userId, UUID contentId, ContentType contentType) {
        try {
            ObjectNode node = objectMapper.createObjectNode();
            node.put("userId", userId.toString());
            node.put("contentId", contentId.toString());
            node.put("contentType", contentType.name());
            node.put("occurredAt", Instant.now().toString());
            kafkaTemplate.send(topic, contentId.toString(), objectMapper.writeValueAsString(node));
        } catch (Exception e) {
            log.error("Failed to publish to topic {}: {}", topic, e.getMessage(), e);
        }
    }

    private void publishDbWrite(String operation, UUID userId, UUID contentId, ContentType contentType) {
        try {
            ObjectNode node = objectMapper.createObjectNode();
            node.put("operation", operation);
            node.put("table", "likes");
            node.put("userId", userId.toString());
            node.put("contentId", contentId.toString());
            node.put("contentType", contentType.name());
            node.put("occurredAt", Instant.now().toString());
            if ("INSERT".equals(operation)) {
                node.put("likeId", UUID.randomUUID().toString());
            }
            kafkaTemplate.send(KafkaTopics.DB_WRITE, contentId.toString(),
                    objectMapper.writeValueAsString(node));
        } catch (Exception e) {
            log.error("Failed to publish db.write event: {}", e.getMessage(), e);
        }
    }

    // -------------------------------------------------------------------------
    // PostgreSQL fallback for likers list
    // -------------------------------------------------------------------------

    private PagedLikersResponse getRecentLikersFromPostgres(UUID contentId, ContentType contentType,
                                                             int limit, int offset) {
        PageRequest pageable = PageRequest.of(offset / Math.max(limit, 1), limit);
        List<Like> likes = likeRepository.findByContentIdAndContentTypeOrderByCreatedAtDesc(
                contentId, contentType, pageable);

        List<String> userIds = likes.stream()
                .map(l -> l.getUserId().toString())
                .collect(Collectors.toList());

        boolean hasMore = userIds.size() == limit;
        String nextCursor = hasMore ? encodeCursorOffset(offset + limit) : null;
        return new PagedLikersResponse(userIds, nextCursor, hasMore);
    }

    // -------------------------------------------------------------------------
    // Cursor encoding/decoding
    // -------------------------------------------------------------------------

    private String encodeCursorOffset(int offset) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(("offset:" + offset).getBytes());
    }

    private int decodeCursorOffset(String cursor) {
        try {
            String decoded = new String(Base64.getUrlDecoder().decode(cursor));
            if (decoded.startsWith("offset:")) {
                return Integer.parseInt(decoded.substring(7));
            }
        } catch (Exception e) {
            log.warn("Invalid cursor: {}", cursor);
        }
        return 0;
    }

    private String encodeCursorInstant(Instant instant) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(("ts:" + instant.toEpochMilli()).getBytes());
    }

    private Instant decodeCursorInstant(String cursor) {
        try {
            String decoded = new String(Base64.getUrlDecoder().decode(cursor));
            if (decoded.startsWith("ts:")) {
                return Instant.ofEpochMilli(Long.parseLong(decoded.substring(3)));
            }
        } catch (Exception e) {
            log.warn("Invalid cursor: {}", cursor);
        }
        return Instant.now();
    }
}
