package com.play.stream.Starjams.FeedService.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Durable follow-graph record in PostgreSQL.
 * Aerospike holds the hot-path Map CDT; this table is the system of record
 * for analytics, billing-level queries, and audit trails.
 */
@Entity
@Table(
    name = "follows",
    indexes = {
        @Index(name = "idx_follows_follower", columnList = "follower_id"),
        @Index(name = "idx_follows_followee", columnList = "followee_id")
    },
    uniqueConstraints = @UniqueConstraint(
        name = "uq_follows_pair",
        columnNames = {"follower_id", "followee_id"}
    )
)
public class Follow {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "follower_id", nullable = false)
    private UUID followerId;

    @Column(name = "followee_id", nullable = false)
    private UUID followeeId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public Follow() {}

    public Follow(UUID followerId, UUID followeeId) {
        this.followerId = followerId;
        this.followeeId = followeeId;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getFollowerId() { return followerId; }
    public void setFollowerId(UUID followerId) { this.followerId = followerId; }
    public UUID getFolloweeId() { return followeeId; }
    public void setFolloweeId(UUID followeeId) { this.followeeId = followeeId; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
