package com.foodya.backend.application.port.out;

import com.foodya.backend.domain.persistence.AuditLog;

public interface AuditLogPort {

    AuditLog save(AuditLog auditLog);
}
