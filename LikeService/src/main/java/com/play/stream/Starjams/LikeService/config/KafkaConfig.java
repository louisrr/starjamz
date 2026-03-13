package com.play.stream.Starjams.LikeService.config;

/**
 * Kafka topic name constants for LikeService.
 */
public final class KafkaTopics {

    private KafkaTopics() {}

    /** Published when a user likes a content item. Consumed by FeedService, NotificationService, EngagementService. */
    public static final String CONTENT_LIKED   = "content.liked";

    /** Published when a user unlikes a content item. Consumed by FeedService, EngagementService. */
    public static final String CONTENT_UNLIKED = "content.unliked";

    /** Async PostgreSQL write queue — INSERT / DELETE likes rows. */
    public static final String DB_WRITE        = "db.write";
}
