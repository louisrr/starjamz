package com.play.stream.Starjams.MediaIngressService.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Lightweight append-only audit trail for all mutating admin actions.
 */
@Entity
@Table(name = "admin_audit_log", indexes = {
    @Index(name = "idx_audit_log_created_at", columnList = "created_at"),
    @Index(name = "idx_audit_log_admin_username", columnList = "admin_username")
})
public class AdminAuditLog {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "admin_username", nullable = false, length = 128)
    private String adminUsername;

    @Column(name = "action", nullable = false, length = 64)
    private String action;

    /** The stream key, VOD ID, or other resource identifier that was affected. */
    @Column(name = "target_id", length = 128)
    private String targetId;

    @Column(name = "details", columnDefinition = "TEXT")
    private String details;

    @Column(name = "created_at")
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (id == null) id = UUID.randomUUID();
        if (createdAt == null) createdAt = Instant.now();
    }

    // --- Getters & Setters ---

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getAdminUsername() { return adminUsername; }
    public void setAdminUsername(String adminUsername) { this.adminUsername = adminUsername; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getTargetId() { return targetId; }
    public void setTargetId(String targetId) { this.targetId = targetId; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
