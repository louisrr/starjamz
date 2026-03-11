package com.play.stream.Starjams.FeedService.controller;

import com.play.stream.Starjams.FeedService.dto.FeedPage;
import com.play.stream.Starjams.FeedService.services.FeedReadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.UUID;

/**
 * Unified activity feed endpoint.
 *
 * <pre>
 *   GET /api/v1/users/{userId}/feed?cursor=&limit=20
 * </pre>
 *
 * Returns a single ranked array of heterogeneous cards:
 * FeedEvent, DigestCard, LivestreamCard, TrendingUserCard, and popular-content cards.
 * The {@code cardType} field on each item acts as a discriminator for the client.
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Feed", description = "Unified activity feed for a user")
public class FeedController {

    private final FeedReadService feedRead;

    public FeedController(FeedReadService feedRead) {
        this.feedRead = feedRead;
    }

    @Operation(
        summary = "Get unified ranked feed for a user",
        description = "Returns a mix of posts, reposts, activity events, digest cards, " +
                      "trending user cards, and popular content. Sorted by the per-user " +
                      "ranking algorithm. Uses cursor-based pagination."
    )
    @GetMapping("/users/{userId}/feed")
    public ResponseEntity<FeedPage> getFeed(
            @PathVariable UUID userId,
            @Parameter(description = "Opaque cursor from previous page. Omit for first page.")
            @RequestParam(required = false) String cursor,
            @Parameter(description = "Page size (1–100)")
            @RequestParam(defaultValue = "20") int limit) {

        if (limit < 1 || limit > 100) {
            return ResponseEntity.badRequest().build();
        }

        FeedReadService.FeedReadResult result = feedRead.read(userId, cursor, limit);

        FeedPage page = new FeedPage(
            new ArrayList<>(result.events()),
            result.nextCursor(),
            limit
        );
        return ResponseEntity.ok(page);
    }
}
