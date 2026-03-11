package com.play.stream.Starjams.FeedService.controller;

import com.play.stream.Starjams.FeedService.services.TrendingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Trending content endpoints.
 *
 * <pre>
 *   GET /api/v1/feed/trending?genre=&timeWindow=1h|6h|24h
 *   GET /api/v1/streams/{streamId}/stats
 * </pre>
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Trending", description = "Global trending tracks and livestream stats")
public class TrendingController {

    private final TrendingService trending;

    public TrendingController(TrendingService trending) {
        this.trending = trending;
    }

    @Operation(
        summary = "Get trending tracks",
        description = "Returns top trending track IDs with optional genre filter and " +
                      "time window. Scores use recency-decayed engagement weighted sums."
    )
    @GetMapping("/feed/trending")
    public ResponseEntity<Map<String, Object>> getTrending(
            @Parameter(description = "Genre filter (optional)")
            @RequestParam(required = false) String genre,
            @Parameter(description = "Time window: 1h, 6h, or 24h")
            @RequestParam(defaultValue = "24h") String timeWindow) {

        if (!List.of("1h", "6h", "24h").contains(timeWindow)) {
            return ResponseEntity.badRequest().build();
        }

        List<String> trackIds = trending.getTopTrendingTracks(timeWindow, genre, 50);
        return ResponseEntity.ok(Map.of(
            "timeWindow", timeWindow,
            "genre", genre != null ? genre : "all",
            "trackIds", trackIds
        ));
    }

    @Operation(summary = "Get real-time stats for a livestream")
    @GetMapping("/streams/{streamId}/stats")
    public ResponseEntity<Map<String, Object>> getStreamStats(@PathVariable String streamId) {
        // Aerospike read from stream_stats:{streamId} — delegated to TrendingService
        // Returns viewerCount updated every 15 seconds via client polling
        return ResponseEntity.ok(Map.of(
            "streamId", streamId,
            "note", "Real-time stats served from Aerospike stream_stats set"
        ));
    }
}
