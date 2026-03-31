package com.foodya.backend.infrastructure.repository;

import com.foodya.backend.domain.entities.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

	long deleteByCreatedAtBefore(OffsetDateTime cutoff);
}
