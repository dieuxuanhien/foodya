package com.foodya.backend.infrastructure.adapter.persistence;

import com.foodya.backend.application.port.out.AuditLogPort;
import com.foodya.backend.domain.persistence.AuditLog;
import com.foodya.backend.infrastructure.repository.AuditLogRepository;
import org.springframework.stereotype.Component;

@Component
public class AuditLogPersistenceAdapter implements AuditLogPort {

    private final AuditLogRepository repository;

    public AuditLogPersistenceAdapter(AuditLogRepository repository) {
        this.repository = repository;
    }

    @Override
    public AuditLog save(AuditLog auditLog) {
        return repository.save(auditLog);
    }
}
