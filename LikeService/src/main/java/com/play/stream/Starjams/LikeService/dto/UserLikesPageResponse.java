package com.play.stream.Starjams.LikeService.dto;

import java.util.List;

public record UserLikesPageResponse(List<LikedContentResponse> items, String nextCursor, boolean hasMore) {}
