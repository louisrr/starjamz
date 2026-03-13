package com.play.stream.Starjams.LikeService;

import com.play.stream.Starjams.LikeService.dto.LikeCountResponse;
import com.play.stream.Starjams.LikeService.dto.LikeStatusResponse;
import com.play.stream.Starjams.LikeService.dto.PagedLikersResponse;
import com.play.stream.Starjams.LikeService.dto.UserLikesPageResponse;
import com.play.stream.Starjams.LikeService.model.ContentType;

import java.util.UUID;

public interface LikeService {

    /**
     * Like a content item. Idempotent — calling twice has no side effects.
     */
    void like(UUID userId, UUID contentId, ContentType contentType);

    /**
     * Unlike a content item. Idempotent — calling when not liked has no side effects.
     */
    void unlike(UUID userId, UUID contentId, ContentType contentType);

    /**
     * Returns the current total like count for a content item (Aerospike only).
     */
    LikeCountResponse getLikeCount(UUID contentId, ContentType contentType);

    /**
     * Returns whether the user has liked the item and the total like count (Aerospike only).
     */
    LikeStatusResponse getLikeStatus(UUID userId, UUID contentId, ContentType contentType);

    /**
     * Returns a paginated list of user IDs who liked a content item.
     * Recent likers are served from Aerospike; full history falls back to PostgreSQL.
     */
    PagedLikersResponse getRecentLikers(UUID contentId, ContentType contentType, int limit, String cursor);

    /**
     * Returns all content items liked by a user, filtered by content type, paginated.
     * Served from PostgreSQL (cold read path).
     */
    UserLikesPageResponse getUserLikes(UUID userId, ContentType contentType, int limit, String cursor);
}
