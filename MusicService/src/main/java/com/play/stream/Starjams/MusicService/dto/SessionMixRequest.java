package com.play.stream.Starjams.MusicService.dto;

import java.util.Map;
import java.util.UUID;

/**
 * Body for PATCH /stem-session/{sessionId}/mix — updates the shared stem volumes.
 */
public record SessionMixRequest(
        UUID participantUserId,
        Map<String, Double> stemVolumes
) {}
