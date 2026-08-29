package com.oms.audit.repository;

import com.oms.audit.entity.AuditLog;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Repository for {@link AuditLog} entity.
 * <p>
 * The audit log table is append-only; no update or delete methods are
 * exposed to enforce immutability (F-24 remediation).
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {
}
