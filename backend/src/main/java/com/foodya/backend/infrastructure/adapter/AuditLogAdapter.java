package com.foodya.backend.infrastructure.adapter;

import com.foodya.backend.application.ports.out.AuditLogPort;
import com.foodya.backend.domain.entities.AuditLog;
import com.foodya.backend.infrastructure.repository.AuditLogRepository;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.Objects;

@Component
public class AuditLogAdapter implements AuditLogPort {

    private final AuditLogRepository repository;

    public AuditLogAdapter(AuditLogRepository repository) {
        this.repository = repository;
    }

    @Override
    public void save(AuditLog auditLog) {
        AuditLog payload = Objects.requireNonNull(auditLog);
        repository.save(payload);
    }

    @Override
    public long deleteByCreatedAtBefore(OffsetDateTime cutoff) {
        return repository.deleteByCreatedAtBefore(Objects.requireNonNull(cutoff));
    }
}
