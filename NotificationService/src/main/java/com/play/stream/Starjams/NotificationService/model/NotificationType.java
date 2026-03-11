package com.play.stream.Starjams.NotificationService.model;

/**
 * All notification types that NotificationService can deliver.
 * Produced by any other service and published to the {@code notification.event} Kafka topic.
 */
public enum NotificationType {

    // Follow graph
    NEW_FOLLOWER,              // someone followed you

    // Feed events
    TRACK_LIKED,               // someone liked your track
    TRACK_REPOSTED,            // someone reposted your track ("Flipped It")
    TRACK_COMMENTED,           // someone commented on your track
    VIDEO_LIKED,
    VIDEO_COMMENTED,

    // Viral mechanics
    PLAY_STREAK,               // user played same artist N days in a row → follow nudge
    GIFT_UNLOCK,               // gifting threshold met — track is now unlocked
    TRACK_BUZZING,             // your track hit 100 plays in 6 h

    // Livestreams
    LIVESTREAM_STARTED,        // someone you follow went live
    LIVESTREAM_ENDED,          // live session ended (replay available)

    // Playlists
    PLAYLIST_FOLLOWED,         // someone followed your playlist
    PLAYLIST_TRACK_ADDED,      // collaborator added a track to shared playlist

    // Collab chain
    COLLAB_TAG,                // you were tagged as a collaborator on a post

    // Gifting
    GIFT_RECEIVED,             // you received a gift on Fetio

    // System
    WELCOME,                   // onboarding notification
    TRENDING_ARTIST_ALERT      // artist you follow is trending
}
