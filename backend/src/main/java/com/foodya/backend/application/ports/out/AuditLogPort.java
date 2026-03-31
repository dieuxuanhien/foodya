package com.foodya.backend.application.ports.out;

import com.foodya.backend.domain.entities.AuditLog;

import java.time.OffsetDateTime;

public interface AuditLogPort {

    AuditLog save(AuditLog auditLog);

    long deleteByCreatedAtBefore(OffsetDateTime cutoff);
}
