package com.play.stream.Starjams.MusicService.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Persistent record of micro-royalties earned by a host per listener-minute
 * in a collaborative stem session.
 *
 * Populated by StemSessionService's hourly Aerospike→PostgreSQL flush.
 */
@Entity
@Table(name = "stem_royalty_ledger")
public class StemRoyaltyLedger {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "session_id", nullable = false)
    private UUID sessionId;

    @Column(name = "host_user_id", nullable = false)
    private UUID hostUserId;

    @Column(name = "listener_user_id", nullable = false)
    private UUID listenerUserId;

    @Column(name = "listener_minutes", nullable = false, precision = 10, scale = 4)
    private BigDecimal listenerMinutes;

    @Column(name = "royalty_amount", nullable = false, precision = 12, scale = 6)
    private BigDecimal royaltyAmount;

    @Column(name = "settled_at", nullable = false)
    private Instant settledAt;

    public StemRoyaltyLedger() {}

    public StemRoyaltyLedger(UUID sessionId, UUID hostUserId, UUID listenerUserId,
                              BigDecimal listenerMinutes, BigDecimal royaltyAmount) {
        this.sessionId       = sessionId;
        this.hostUserId      = hostUserId;
        this.listenerUserId  = listenerUserId;
        this.listenerMinutes = listenerMinutes;
        this.royaltyAmount   = royaltyAmount;
        this.settledAt       = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getSessionId() { return sessionId; }
    public UUID getHostUserId() { return hostUserId; }
    public UUID getListenerUserId() { return listenerUserId; }
    public BigDecimal getListenerMinutes() { return listenerMinutes; }
    public BigDecimal getRoyaltyAmount() { return royaltyAmount; }
    public Instant getSettledAt() { return settledAt; }
}
