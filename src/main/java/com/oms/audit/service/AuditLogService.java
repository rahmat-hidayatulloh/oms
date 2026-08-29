package com.oms.audit.service;

import com.oms.audit.entity.AuditLog;
import com.oms.audit.repository.AuditLogRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;

import lombok.extern.slf4j.Slf4j;

/**
 * Service for recording immutable audit log entries.
 * <p>
 * Provides both programmatic and annotation-driven audit logging.
 * All entries are persisted to the {@code audit_log} table which is
 * append-only by convention (F-24 remediation).
 * <p>
 * Declarative usage via {@link com.oms.audit.annotation.Auditable} is
 * handled by {@link com.oms.audit.aspect.AuditLogAspect}.
 *
 * @see com.oms.audit.annotation.Auditable
 * @see com.oms.audit.aspect.AuditLogAspect
 */
@Slf4j
@Service
public class AuditLogService {

    private static final ZoneId APP_TIMEZONE = ZoneId.of("Asia/Jakarta");

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    /**
     * Records an audit log entry.
     *
     * @param event the audit event details
     */
    @Transactional
    public void record(AuditEvent event) {
        AuditLog auditLog = new AuditLog(
                LocalDateTime.now(APP_TIMEZONE),
                event.userId(),
                event.action(),
                event.entityType(),
                event.entityId(),
                event.ipAddress(),
                event.details()
        );

        auditLogRepository.save(auditLog);
        log.debug("Audit log recorded: action={}, entityType={}, "
                + "entityId={}, user={}",
                event.action(), event.entityType(),
                event.entityId(), event.userId());
    }

    /**
     * Audit event record that bundles all audit log entry parameters.
     *
     * @param action the action performed (e.g., CREATE, UPDATE, LOGIN)
     * @param entityType the entity type affected (e.g., Product, Order)
     * @param entityId the entity ID (may be null)
     * @param userId the user who performed the action (may be null)
     * @param ipAddress the client IP address (may be null)
     * @param details additional context about the event
     */
    public record AuditEvent(String action, String entityType,
                             String entityId, String userId,
                             String ipAddress, String details) {
    }
}
