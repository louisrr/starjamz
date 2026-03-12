package com.play.stream.Starjams.FeedService.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * A remix card is a derived work created when a user saves a named stem mix configuration.
 * It links back to the original track and persists the stem volume snapshot used.
 *
 * Maps to: remix_cards (migration V2)
 */
@Entity
@Table(name = "remix_cards")
public class RemixCard {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "original_track_id", nullable = false)
    private UUID originalTrackId;

    @Column(name = "remixer_user_id", nullable = false)
    private UUID remixerUserId;

    @Column(name = "remix_title")
    private String remixTitle;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "stem_volumes_json", nullable = false, columnDefinition = "jsonb")
    private Map<String, Double> stemVolumes;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "total_gifts_received", nullable = false)
    private int totalGiftsReceived = 0;

    public RemixCard() {}

    public RemixCard(UUID originalTrackId, UUID remixerUserId,
                     String remixTitle, Map<String, Double> stemVolumes) {
        this.originalTrackId = originalTrackId;
        this.remixerUserId   = remixerUserId;
        this.remixTitle      = remixTitle;
        this.stemVolumes     = stemVolumes;
        this.createdAt       = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getOriginalTrackId() { return originalTrackId; }
    public UUID getRemixerUserId() { return remixerUserId; }
    public String getRemixTitle() { return remixTitle; }
    public Map<String, Double> getStemVolumes() { return stemVolumes; }
    public Instant getCreatedAt() { return createdAt; }
    public int getTotalGiftsReceived() { return totalGiftsReceived; }
    public void setTotalGiftsReceived(int totalGiftsReceived) {
        this.totalGiftsReceived = totalGiftsReceived;
    }
}
