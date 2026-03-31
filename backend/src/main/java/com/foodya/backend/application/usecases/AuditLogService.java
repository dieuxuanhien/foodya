package com.foodya.backend.application.usecases;

import com.foodya.backend.application.ports.out.AuditLogPort;
import com.foodya.backend.domain.entities.AuditLog;
import org.springframework.stereotype.Service;

@Service
public class AuditLogService {

    private final AuditLogPort auditLogPort;

    public AuditLogService(AuditLogPort auditLogPort) {
        this.auditLogPort = auditLogPort;
    }

    public void securityEvent(String actor,
                              String action,
                              String targetType,
                              String targetId,
                              String oldValue,
                              String newValue) {
        auditLogPort.save(AuditLog.securityEvent(actor, action, targetType, targetId, oldValue, newValue));
    }
}
