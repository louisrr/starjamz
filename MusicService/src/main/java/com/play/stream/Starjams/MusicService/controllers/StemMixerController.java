package com.play.stream.Starjams.MusicService.controllers;

import com.play.stream.Starjams.MusicService.dto.SaveRemixRequest;
import com.play.stream.Starjams.MusicService.dto.StemMixRequest;
import com.play.stream.Starjams.MusicService.services.StemMixerService;
import com.play.stream.Starjams.MusicService.services.StemTierResolver;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.util.Map;
import java.util.UUID;

/**
 * REST endpoints for the per-user stem mixer.
 *
 * GET  /stream/{trackId}/mix          — stream the mixed audio (defaults to full_mix)
 * PATCH /stream/{trackId}/mix         — update stem volumes and stream the new mix
 * POST /stream/{trackId}/remix        — save a named mix as a remix card (Kafka event)
 * GET  /stream/{trackId}/tier         — returns the caller's current unlock tier
 */
@RestController
@RequestMapping("/stream")
public class StemMixerController {

    private static final String REMIX_TOPIC = "remix.created";

    private final StemMixerService  mixerService;
    private final StemTierResolver  tierResolver;
    private final KafkaTemplate<String, String> kafka;

    public StemMixerController(StemMixerService mixerService,
                                StemTierResolver tierResolver,
                                KafkaTemplate<String, String> kafka) {
        this.mixerService  = mixerService;
        this.tierResolver  = tierResolver;
        this.kafka         = kafka;
    }

    /**
     * Streams the full mixed audio for a track using the default (flat) stem volumes.
     * The user gets only the stems their unlock tier permits.
     */
    @GetMapping(value = "/{trackId}/mix", produces = "audio/mpeg")
    public ResponseEntity<StreamingResponseBody> streamDefaultMix(
            @PathVariable UUID trackId,
            @RequestParam UUID userId) {

        StreamingResponseBody body = mixerService.streamMix(
                trackId, userId, Map.of());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("audio/mpeg"))
                .body(body);
    }

    /**
     * Accepts new per-stem volume settings, rebuilds the GStreamer pipeline,
     * and streams the updated mix. Only accessible stems are honoured.
     *
     * Body: { "stemVolumes": { "vocals": 0.8, "drums": 1.0, "bass": 0.6 } }
     */
    @PatchMapping(value = "/{trackId}/mix",
                  consumes = MediaType.APPLICATION_JSON_VALUE,
                  produces = "audio/mpeg")
    public ResponseEntity<StreamingResponseBody> updateMix(
            @PathVariable UUID trackId,
            @RequestParam UUID userId,
            @RequestBody StemMixRequest request) {

        // Validate volumes
        if (request.stemVolumes() != null) {
            for (Map.Entry<String, Double> e : request.stemVolumes().entrySet()) {
                if (!tierResolver.canAccessStem(userId, trackId, e.getKey())) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .build();
                }
                double v = e.getValue();
                if (v < 0.0 || v > 1.0) {
                    return ResponseEntity.badRequest().build();
                }
            }
        }

        mixerService.updateMix(trackId, userId,
                request.stemVolumes() != null ? request.stemVolumes() : Map.of());

        StreamingResponseBody body = mixerService.streamMix(
                trackId, userId, request.stemVolumes() != null
                        ? request.stemVolumes() : Map.of());

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("audio/mpeg"))
                .body(body);
    }

    /**
     * Saves the current stem mix as a named remix card.
     * Publishes a remix.created event to Kafka so FeedService can fan it out.
     */
    @PostMapping(value = "/{trackId}/remix",
                 consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> saveRemix(
            @PathVariable UUID trackId,
            @RequestBody SaveRemixRequest request) {

        UUID remixId = UUID.randomUUID();

        String payload = String.format(
                "{\"remixId\":\"%s\",\"originalTrackId\":\"%s\","
              + "\"remixerUserId\":\"%s\",\"remixTitle\":\"%s\","
              + "\"stemVolumes\":%s}",
                remixId,
                trackId,
                request.remixerUserId(),
                escapeJson(request.remixTitle()),
                volumesToJson(request.stemVolumes()));

        kafka.send(REMIX_TOPIC, remixId.toString(), payload);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("remixId", remixId, "status", "queued"));
    }

    /**
     * Returns the calling user's current stem unlock tier for the given track.
     */
    @GetMapping("/{trackId}/tier")
    public ResponseEntity<?> getTier(
            @PathVariable UUID trackId,
            @RequestParam UUID userId) {

        int tier = tierResolver.getUnlockTier(userId, trackId);
        return ResponseEntity.ok(Map.of(
                "tier",          tier,
                "accessibleStems", tierResolver.accessibleStems(userId, trackId),
                "nextTierCost",  StemTierResolver.giftsRequiredForTier(tier + 1)));
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private String escapeJson(String s) {
        return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String volumesToJson(Map<String, Double> volumes) {
        if (volumes == null || volumes.isEmpty()) return "{}";
        StringBuilder sb = new StringBuilder("{");
        volumes.forEach((k, v) -> sb.append("\"").append(k).append("\":").append(v).append(","));
        sb.setCharAt(sb.length() - 1, '}');
        return sb.toString();
    }
}
