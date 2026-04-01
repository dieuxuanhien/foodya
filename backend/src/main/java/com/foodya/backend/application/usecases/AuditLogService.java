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
        // Application layer maps to domain entity before crossing port boundary
        AuditLog auditLog = AuditLog.securityEvent(actor, action, targetType, targetId, oldValue, newValue);
        auditLogPort.save(auditLog);
    }
}
