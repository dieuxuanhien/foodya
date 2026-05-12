package com.foodya.backend.infrastructure.adapter;

import com.foodya.backend.application.ports.out.AuditLogPort;
import com.foodya.backend.application.dto.PaginatedResult;
import com.foodya.backend.domain.entities.AuditLog;
import com.foodya.backend.infrastructure.mapper.AuditLogMapper;
import com.foodya.backend.infrastructure.repository.AuditLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.Objects;

@Component
public class AuditLogAdapter implements AuditLogPort {

    private final AuditLogRepository repository;
    private final AuditLogMapper mapper;

    public AuditLogAdapter(AuditLogRepository repository,
                           AuditLogMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public void save(AuditLog auditLog) {
        AuditLog payload = Objects.requireNonNull(auditLog);
        repository.save(Objects.requireNonNull(mapper.toPersistence(payload)));
    }

    @Override
    public PaginatedResult<AuditLog> list(String action, String targetType, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<AuditLog> result = repository.search(action, targetType, pageable).map(mapper::toDomain);
        return new PaginatedResult<>(
                result.getContent(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    @Override
    public long deleteByCreatedAtBefore(OffsetDateTime cutoff) {
        return repository.deleteByCreatedAtBefore(Objects.requireNonNull(cutoff));
    }
}
