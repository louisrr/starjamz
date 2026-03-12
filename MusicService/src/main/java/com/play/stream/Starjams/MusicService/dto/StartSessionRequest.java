package com.play.stream.Starjams.MusicService.dto;

import java.util.Map;
import java.util.UUID;

/**
 * Body for POST /stem-session — creates a new collaborative stem session.
 */
public record StartSessionRequest(
        UUID trackId,
        UUID hostUserId,
        Map<String, Double> initialStemVolumes   // optional; defaults to all 1.0
) {}
