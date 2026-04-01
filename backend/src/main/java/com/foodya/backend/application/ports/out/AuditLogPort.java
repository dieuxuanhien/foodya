package com.foodya.backend.application.ports.out;

import com.foodya.backend.domain.entities.AuditLog;

import java.time.OffsetDateTime;

/**
 * Output port for audit log persistence.
 * Abstracts the audit storage mechanism from application logic.
 * Depends only on domain entities to maintain inward dependency flow.
 */
public interface AuditLogPort {

    void save(AuditLog auditLog);

    long deleteByCreatedAtBefore(OffsetDateTime cutoff);
}
