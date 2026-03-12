package com.play.stream.Starjams.UploadService.dto;

import java.util.Map;

/**
 * Response received from the Demucs sidecar service after stem separation.
 * Maps stem type names (e.g. "vocals") to the S3 key of the separated stem file.
 */
public record StemSeparationResponse(Map<String, String> stems) {}
