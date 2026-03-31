package com.foodya.backend.application.port.out;

import com.foodya.backend.domain.persistence.AuditLog;

import java.time.OffsetDateTime;

public interface AuditLogPort {

    AuditLog save(AuditLog auditLog);

    long deleteByCreatedAtBefore(OffsetDateTime cutoff);
}
