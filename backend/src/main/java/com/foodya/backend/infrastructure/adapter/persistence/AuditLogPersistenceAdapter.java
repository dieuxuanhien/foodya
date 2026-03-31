package com.foodya.backend.infrastructure.adapter.persistence;

import com.foodya.backend.application.ports.out.AuditLogPort;
import com.foodya.backend.domain.entities.AuditLog;
import com.foodya.backend.infrastructure.repository.AuditLogRepository;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.Objects;

@Component
public class AuditLogPersistenceAdapter implements AuditLogPort {

    private final AuditLogRepository repository;

    public AuditLogPersistenceAdapter(AuditLogRepository repository) {
        this.repository = repository;
    }

    @Override
    public AuditLog save(AuditLog auditLog) {
        return repository.save(Objects.requireNonNull(auditLog));
    }

    @Override
    public long deleteByCreatedAtBefore(OffsetDateTime cutoff) {
        return repository.deleteByCreatedAtBefore(Objects.requireNonNull(cutoff));
    }
}
