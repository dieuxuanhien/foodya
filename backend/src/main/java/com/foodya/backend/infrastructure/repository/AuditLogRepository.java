package com.foodya.backend.infrastructure.repository;

import com.foodya.backend.infrastructure.persistence.models.AuditLogPersistenceModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLogPersistenceModel, UUID> {

		@Query("""
						select a from AuditLogPersistenceModel a
						where (:action is null or a.action = :action)
							and (:targetType is null or a.targetType = :targetType)
						""")
		Page<AuditLogPersistenceModel> search(@Param("action") String action,
																					@Param("targetType") String targetType,
																					Pageable pageable);

	long deleteByCreatedAtBefore(OffsetDateTime cutoff);
}
