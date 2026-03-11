package com.play.stream.Starjams.FeedService.controller;

import com.play.stream.Starjams.FeedService.dto.PrivacySettingsRequest;
import com.play.stream.Starjams.FeedService.services.FollowGraphService;
import com.play.stream.Starjams.FeedService.services.PrivacyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST API for the asymmetric follow graph and user privacy settings.
 *
 * <pre>
 *   POST   /api/v1/users/{userId}/follow/{targetId}
 *   DELETE /api/v1/users/{userId}/follow/{targetId}
 *   GET    /api/v1/users/{userId}/following?offset=0&limit=20
 *   GET    /api/v1/users/{userId}/followers?offset=0&limit=20
 *   GET    /api/v1/users/{userId}/following/{targetId}/status
 *   POST   /api/v1/users/{userId}/privacy
 *   GET    /api/v1/users/{userId}/privacy
 * </pre>
 */
@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Follow Graph", description = "Asymmetric follow relationships and privacy settings")
public class FollowController {

    private final FollowGraphService followGraph;
    private final PrivacyService     privacy;

    public FollowController(FollowGraphService followGraph, PrivacyService privacy) {
        this.followGraph = followGraph;
        this.privacy     = privacy;
    }

    // -------------------------------------------------------------------------
    // Follow / Unfollow
    // -------------------------------------------------------------------------

    @Operation(summary = "Follow a user")
    @PostMapping("/{userId}/follow/{targetId}")
    public ResponseEntity<Void> follow(
            @PathVariable UUID userId,
            @PathVariable UUID targetId) {
        followGraph.follow(userId, targetId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Unfollow a user")
    @DeleteMapping("/{userId}/follow/{targetId}")
    public ResponseEntity<Void> unfollow(
            @PathVariable UUID userId,
            @PathVariable UUID targetId) {
        followGraph.unfollow(userId, targetId);
        return ResponseEntity.noContent().build();
    }

    // -------------------------------------------------------------------------
    // Following list (paginated)
    // -------------------------------------------------------------------------

    @Operation(summary = "List users that userId is following")
    @GetMapping("/{userId}/following")
    public ResponseEntity<Map<String, Object>> getFollowing(
            @PathVariable UUID userId,
            @Parameter(description = "Zero-based offset") @RequestParam(defaultValue = "0") int offset,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int limit) {

        List<UUID> ids   = followGraph.getFollowingPage(userId, offset, limit);
        long total       = followGraph.getFollowingCount(userId);
        return ResponseEntity.ok(Map.of(
            "userId", userId,
            "following", ids,
            "total", total,
            "offset", offset,
            "limit", limit
        ));
    }

    // -------------------------------------------------------------------------
    // Followers list (paginated)
    // -------------------------------------------------------------------------

    @Operation(summary = "List users who follow userId")
    @GetMapping("/{userId}/followers")
    public ResponseEntity<Map<String, Object>> getFollowers(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "20") int limit) {

        List<UUID> ids = followGraph.getFollowersPage(userId, offset, limit);
        long total     = followGraph.getFollowerCount(userId);
        return ResponseEntity.ok(Map.of(
            "userId", userId,
            "followers", ids,
            "total", total,
            "offset", offset,
            "limit", limit
        ));
    }

    // -------------------------------------------------------------------------
    // Relationship status check
    // -------------------------------------------------------------------------

    @Operation(summary = "Check if userId follows targetId")
    @GetMapping("/{userId}/following/{targetId}/status")
    public ResponseEntity<Map<String, Object>> followStatus(
            @PathVariable UUID userId,
            @PathVariable UUID targetId) {
        boolean isFollowing = followGraph.isFollowing(userId, targetId);
        return ResponseEntity.ok(Map.of(
            "followerId", userId,
            "followeeId", targetId,
            "isFollowing", isFollowing
        ));
    }

    // -------------------------------------------------------------------------
    // Privacy settings
    // -------------------------------------------------------------------------

    @Operation(summary = "Update activity-sharing privacy settings")
    @PostMapping("/{userId}/privacy")
    public ResponseEntity<Void> updatePrivacy(
            @PathVariable UUID userId,
            @RequestBody PrivacySettingsRequest request) {
        privacy.updateSettings(userId, request);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get current privacy settings")
    @GetMapping("/{userId}/privacy")
    public ResponseEntity<PrivacySettingsRequest> getPrivacy(@PathVariable UUID userId) {
        return ResponseEntity.ok(privacy.getSettings(userId));
    }
}
