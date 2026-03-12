package com.play.stream.Starjams.FeedService.config;

/**
 * Kafka topic name constants shared across producers and consumers.
 */
public final class KafkaTopics {

    private KafkaTopics() {}

    // Inbound — consumed by FeedService
    public static final String TRACK_POSTED       = "track.posted";
    public static final String TRACK_ENGAGED      = "track.engaged";
    public static final String VIDEO_POSTED       = "video.posted";
    public static final String LIVESTREAM_EVENT   = "livestream.event";
    public static final String PLAYLIST_EVENT     = "playlist.event";

    // Outbound — produced by FeedService, consumed by NotificationService
    public static final String NOTIFICATION_EVENT = "notification.event";

    // Counter sync to PostgreSQL
    public static final String COUNTER_FLUSH      = "engagement.counter.flush";

    // Stem Economy
    public static final String REMIX_CREATED      = "remix.created";
}
