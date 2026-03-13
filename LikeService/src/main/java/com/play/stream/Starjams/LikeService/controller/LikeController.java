package com.play.stream.Starjams.LikeService.controller;

import com.play.stream.Starjams.LikeService.LikeService;
import com.play.stream.Starjams.LikeService.dto.LikeCountResponse;
import com.play.stream.Starjams.LikeService.dto.LikeStatusResponse;
import com.play.stream.Starjams.LikeService.dto.PagedLikersResponse;
import com.play.stream.Starjams.LikeService.dto.UserLikesPageResponse;
import com.play.stream.Starjams.LikeService.model.ContentType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller for the LikeService.
 *
 * <pre>
 *   POST   /api/v1/likes/{contentType}/{contentId}               — like
 *   DELETE /api/v1/likes/{contentType}/{contentId}               — unlike
 *   GET    /api/v1/likes/{contentType}/{contentId}/count          — total likes (public)
 *   GET    /api/v1/likes/{contentType}/{contentId}/users          — liker list (public)
 *   GET    /api/v1/likes/{contentType}/{contentId}/status         — liked? + count (auth)
 *   GET    /api/v1/users/{userId}/likes                           — user's liked content (auth)
 * </pre>
 */
@RestController
@RequestMapping("/api/v1")
public class LikeController {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE     = 100;

    private final LikeService likeService;

    public LikeController(LikeService likeService) {
        this.likeService = likeService;
    }

    // -------------------------------------------------------------------------
    // POST /api/v1/likes/{contentType}/{contentId}   — like
    // -------------------------------------------------------------------------

    @PostMapping("/likes/{contentType}/{contentId}")
    public ResponseEntity<Void> like(
            @PathVariable ContentType contentType,
            @PathVariable UUID contentId,
            @AuthenticationPrincipal Jwt jwt) {

        UUID userId = extractUserId(jwt);
        likeService.like(userId, contentId, contentType);
        return ResponseEntity.ok().build();
    }

    // -------------------------------------------------------------------------
    // DELETE /api/v1/likes/{contentType}/{contentId}   — unlike
    // -------------------------------------------------------------------------

    @DeleteMapping("/likes/{contentType}/{contentId}")
    public ResponseEntity<Void> unlike(
            @PathVariable ContentType contentType,
            @PathVariable UUID contentId,
            @AuthenticationPrincipal Jwt jwt) {

        UUID userId = extractUserId(jwt);
        likeService.unlike(userId, contentId, contentType);
        return ResponseEntity.ok().build();
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/likes/{contentType}/{contentId}/count   — total likes (public)
    // -------------------------------------------------------------------------

    @GetMapping("/likes/{contentType}/{contentId}/count")
    public ResponseEntity<LikeCountResponse> getLikeCount(
            @PathVariable ContentType contentType,
            @PathVariable UUID contentId) {

        return ResponseEntity.ok(likeService.getLikeCount(contentId, contentType));
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/likes/{contentType}/{contentId}/users   — liker list (public)
    // -------------------------------------------------------------------------

    @GetMapping("/likes/{contentType}/{contentId}/users")
    public ResponseEntity<PagedLikersResponse> getRecentLikers(
            @PathVariable ContentType contentType,
            @PathVariable UUID contentId,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) String cursor) {

        limit = clampPageSize(limit);
        return ResponseEntity.ok(likeService.getRecentLikers(contentId, contentType, limit, cursor));
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/likes/{contentType}/{contentId}/status   — liked? + count (auth)
    // -------------------------------------------------------------------------

    @GetMapping("/likes/{contentType}/{contentId}/status")
    public ResponseEntity<LikeStatusResponse> getLikeStatus(
            @PathVariable ContentType contentType,
            @PathVariable UUID contentId,
            @AuthenticationPrincipal Jwt jwt) {

        UUID userId = extractUserId(jwt);
        return ResponseEntity.ok(likeService.getLikeStatus(userId, contentId, contentType));
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/users/{userId}/likes   — user's liked content (auth)
    // -------------------------------------------------------------------------

    @GetMapping("/users/{userId}/likes")
    public ResponseEntity<UserLikesPageResponse> getUserLikes(
            @PathVariable UUID userId,
            @RequestParam(required = false) ContentType contentType,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) String cursor,
            @AuthenticationPrincipal Jwt jwt) {

        // Users may only query their own likes unless they have elevated privileges
        UUID requesterId = extractUserId(jwt);
        if (!requesterId.equals(userId)) {
            return ResponseEntity.status(403).build();
        }

        limit = clampPageSize(limit);
        return ResponseEntity.ok(likeService.getUserLikes(userId, contentType, limit, cursor));
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private UUID extractUserId(Jwt jwt) {
        String sub = jwt.getSubject();
        return UUID.fromString(sub);
    }

    private int clampPageSize(int requested) {
        return Math.max(1, Math.min(requested, MAX_PAGE_SIZE));
    }
}
