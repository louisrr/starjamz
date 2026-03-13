package com.play.stream.Starjams.LikeService.dto;

import java.util.List;

public record PagedLikersResponse(List<String> userIds, String nextCursor, boolean hasMore) {}
