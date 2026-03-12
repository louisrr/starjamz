package com.play.stream.Starjams.MusicService.dto;

import java.util.Map;
import java.util.UUID;

/**
 * Body for POST /stream/{trackId}/remix — saves a named stem mix as a remix card.
 * This publishes a remix.created Kafka event so FeedService can fan it out.
 */
public record SaveRemixRequest(
        UUID remixerUserId,
        String remixTitle,
        Map<String, Double> stemVolumes
) {}
