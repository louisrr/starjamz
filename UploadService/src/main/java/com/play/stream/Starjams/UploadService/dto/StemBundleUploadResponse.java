package com.play.stream.Starjams.UploadService.dto;

import java.util.Map;
import java.util.UUID;

/**
 * Returned to the caller after a stems bundle is ingested.
 * Contains the generated trackId and the S3 keys for each separated stem.
 */
public record StemBundleUploadResponse(
        UUID trackId,
        UUID uploadId,
        Map<String, String> stemS3Keys,   // stemType → s3Key
        String status                      // "processing" | "ready"
) {}
