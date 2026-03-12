package com.play.stream.Starjams.FeedService.model;

/**
 * All activity event types that can appear in the unified feed.
 */
public enum EventType {

    // Music
    TRACK_POSTED,
    TRACK_LIKED,
    TRACK_PLAYED,
    TRACK_REPOSTED,
    TRACK_REMIXED,

    // Video
    VIDEO_POSTED,
    VIDEO_LIKED,
    VIDEO_VIEWED,

    // Social graph
    ARTIST_FOLLOWED,

    // Playlists
    PLAYLIST_CREATED,
    PLAYLIST_LIKED,
    PLAYLIST_TRACK_ADDED,
    PLAYLIST_COLLABORATIVE_JOINED,
    PLAYLIST_SHARED,

    // Livestreams
    LIVESTREAM_STARTED_AUDIO,
    LIVESTREAM_STARTED_VIDEO,
    LIVESTREAM_ENDED,
    LIVESTREAM_CLIPPED,

    // Synthetic / aggregated
    DIGEST,
    TRENDING_USER,
    POPULAR_TRACK,
    POPULAR_VIDEO,
    POPULAR_PLAYLIST,
    POPULAR_LIVESTREAM
}
