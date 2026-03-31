package com.foodya.backend.infrastructure.repository;

import com.foodya.backend.domain.persistence.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {
}
