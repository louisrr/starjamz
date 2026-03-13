package com.play.stream.Starjams.LikeService.dto;

import com.play.stream.Starjams.LikeService.model.ContentType;

import java.util.UUID;

public record LikeCountResponse(UUID contentId, ContentType contentType, long totalLikes) {}
