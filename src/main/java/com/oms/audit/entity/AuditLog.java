package com.oms.audit.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Immutable audit log entity for recording critical application events.
 * <p>
 * This entity maps to the {@code audit_log} table created by Flyway
 * migration V7. The table is append-only by convention to ensure
 * an immutable audit trail (F-24 remediation).
 * <p>
 * Events are recorded via {@link com.oms.audit.service.AuditLogService} and
 * can be triggered declaratively using the
 * {@link com.oms.audit.annotation.Auditable} annotation.
 */
@Entity
@Table(name = "audit_log")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "event_timestamp", nullable = false, updatable = false)
    private LocalDateTime eventTimestamp;

    @Column(name = "user_id", length = 100, updatable = false)
    private String userId;

    @Column(name = "action", nullable = false, length = 50, updatable = false)
    private String action;

    @Column(name = "entity_type", nullable = false, length = 50, updatable = false)
    private String entityType;

    @Column(name = "entity_id", length = 100, updatable = false)
    private String entityId;

    @Column(name = "ip_address", length = 45, updatable = false)
    private String ipAddress;

    @Column(name = "details", updatable = false)
    private String details;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected AuditLog() {
        // Required by JPA
    }

    /**
     * Constructs an immutable audit log entry.
     *
     * @param eventTimestamp when the event occurred
     * @param userId the user who triggered the event (may be null)
     * @param action the action performed (e.g., CREATE, UPDATE, LOGIN)
     * @param entityType the entity type affected (e.g., Product, Order)
     * @param entityId the entity ID (may be null)
     * @param ipAddress the client IP address (may be null)
     * @param details additional details about the event
     */
    public AuditLog(LocalDateTime eventTimestamp, String userId, String action,
                    String entityType, String entityId, String ipAddress,
                    String details) {
        this.eventTimestamp = eventTimestamp;
        this.userId = userId;
        this.action = action;
        this.entityType = entityType;
        this.entityId = entityId;
        this.ipAddress = ipAddress;
        this.details = details;
        this.createdAt = LocalDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public LocalDateTime getEventTimestamp() {
        return eventTimestamp;
    }

    public String getUserId() {
        return userId;
    }

    public String getAction() {
        return action;
    }

    public String getEntityType() {
        return entityType;
    }

    public String getEntityId() {
        return entityId;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public String getDetails() {
        return details;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
