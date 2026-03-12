package com.play.stream.Starjams.MusicService.dto;

import java.util.Map;

/**
 * Body of PATCH /stream/{trackId}/mix.
 * stemVolumes maps each stem name to a gain value in [0.0, 1.0].
 * Omitted stems default to their current value (or 1.0 on first call).
 *
 * Example:
 *   { "stemVolumes": { "vocals": 0.8, "drums": 1.0, "bass": 0.6 } }
 */
public record StemMixRequest(Map<String, Double> stemVolumes) {}
