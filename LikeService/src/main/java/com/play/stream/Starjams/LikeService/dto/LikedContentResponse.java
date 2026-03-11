package com.play.stream.Starjams.LikeService.dto;

import com.play.stream.Starjams.LikeService.model.ContentType;

import java.time.Instant;
import java.util.UUID;

public record LikedContentResponse(UUID likeId, UUID contentId, ContentType contentType, Instant likedAt) {}
