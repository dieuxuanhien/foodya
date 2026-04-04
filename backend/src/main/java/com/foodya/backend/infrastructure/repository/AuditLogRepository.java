package com.foodya.backend.infrastructure.repository;

import com.foodya.backend.infrastructure.persistence.models.AuditLogPersistenceModel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLogPersistenceModel, UUID> {

	long deleteByCreatedAtBefore(OffsetDateTime cutoff);
}
