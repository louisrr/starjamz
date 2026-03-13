package com.play.stream.Starjams.MusicService.controllers;

import com.play.stream.Starjams.MusicService.dto.SessionMixRequest;
import com.play.stream.Starjams.MusicService.dto.StartSessionRequest;
import com.play.stream.Starjams.MusicService.services.StemMixerService;
import com.play.stream.Starjams.MusicService.services.StemSessionService;
import com.play.stream.Starjams.MusicService.services.StemTierResolver;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * REST endpoints for collaborative stem sessions.
 *
 * POST   /stem-session                          — create a session (Tier 2 required)
 * POST   /stem-session/{sessionId}/join         — join an existing session
 * PATCH  /stem-session/{sessionId}/mix          — update shared stem volumes
 * GET    /stem-session/{sessionId}              — fetch current session state
 * DELETE /stem-session/{sessionId}              — end session (host only)
 * GET    /stem-session/{sessionId}/stream       — stream the session mix
 */
@RestController
@RequestMapping("/stem-session")
public class StemSessionController {

    private static final int TIER_REQUIRED_TO_HOST = 2;

    private final StemSessionService sessionService;
    private final StemMixerService   mixerService;
    private final StemTierResolver   tierResolver;

    public StemSessionController(StemSessionService sessionService,
                                  StemMixerService mixerService,
                                  StemTierResolver tierResolver) {
        this.sessionService = sessionService;
        this.mixerService   = mixerService;
        this.tierResolver   = tierResolver;
    }

    /**
     * Creates a new collaborative session. The host must hold at least Tier 2 access.
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> createSession(@RequestBody StartSessionRequest request) {
        int tier = tierResolver.getUnlockTier(request.hostUserId(), request.trackId());
        if (tier < TIER_REQUIRED_TO_HOST) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Tier 2 stem access required to host a session. "
                        + "Current tier: " + tier);
        }

        UUID sessionId = sessionService.createSession(
                request.trackId(),
                request.hostUserId(),
                request.initialStemVolumes());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("sessionId", sessionId));
    }

    /**
     * Joins an existing session as a listener.
     */
    @PostMapping("/{sessionId}/join")
    public ResponseEntity<?> joinSession(
            @PathVariable UUID sessionId,
            @RequestParam UUID userId) {

        boolean joined = sessionService.joinSession(sessionId, userId);
        if (!joined) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Session is full (max 8 participants) or not found");
        }
        return ResponseEntity.ok(Map.of("sessionId", sessionId, "userId", userId));
    }

    /**
     * Updates the shared stem volumes for all participants.
     * Any participant in the session may call this.
     */
    @PatchMapping(value = "/{sessionId}/mix",
                  consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> updateMix(
            @PathVariable UUID sessionId,
            @RequestBody SessionMixRequest request) {

        try {
            Map<String, Double> updated = sessionService.updateSessionMix(
                    sessionId, request.participantUserId(), request.stemVolumes());
            return ResponseEntity.ok(Map.of("stemVolumes", updated));
        } catch (java.util.NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
    }

    /**
     * Returns the current session state (participants, stem volumes, trackId).
     */
    @GetMapping("/{sessionId}")
    public ResponseEntity<?> getSession(@PathVariable UUID sessionId) {
        Map<String, Object> state = sessionService.getSession(sessionId);
        if (state == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(state);
    }

    /**
     * Streams the current session mix for a listener.
     * Uses the shared stem volumes from Aerospike session state.
     */
    @GetMapping(value = "/{sessionId}/stream", produces = "audio/mpeg")
    public ResponseEntity<?> streamSessionMix(
            @PathVariable UUID sessionId,
            @RequestParam UUID userId) {

        Map<String, Object> state = sessionService.getSession(sessionId);
        if (state == null) return ResponseEntity.notFound().build();

        UUID trackId = UUID.fromString((String) state.get("trackId"));
        @SuppressWarnings("unchecked")
        Map<String, Double> volumes = (Map<String, Double>) state.get("stemVolumes");

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("audio/mpeg"))
                .body(mixerService.streamMix(trackId, userId, volumes));
    }

    /**
     * Ends the session (host-only). Tears down the active pipeline.
     */
    @DeleteMapping("/{sessionId}")
    public ResponseEntity<?> endSession(
            @PathVariable UUID sessionId,
            @RequestParam UUID userId) {

        Map<String, Object> state = sessionService.getSession(sessionId);
        if (state == null) return ResponseEntity.notFound().build();

        if (!userId.toString().equals(state.get("hostUserId"))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Only the host can end the session");
        }

        // Find the trackId for teardown
        UUID trackId = UUID.fromString((String) state.get("trackId"));
        mixerService.tearDownSession(userId + ":" + trackId);

        return ResponseEntity.noContent().build();
    }
}
