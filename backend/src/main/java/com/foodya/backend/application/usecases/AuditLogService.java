package com.foodya.backend.application.usecases;

import com.foodya.backend.application.dto.AuditLogView;
import com.foodya.backend.application.dto.PaginatedResult;
import com.foodya.backend.application.ports.in.AuditLogUseCase;
import com.foodya.backend.application.ports.out.AuditLogPort;
import com.foodya.backend.domain.entities.AuditLog;
import com.foodya.backend.application.support.PaginationPolicy;

public class AuditLogService implements AuditLogUseCase {

    private final AuditLogPort auditLogPort;
    private final PaginationPolicy paginationPolicy;

    public AuditLogService(AuditLogPort auditLogPort, PaginationPolicy paginationPolicy) {
        this.auditLogPort = auditLogPort;
        this.paginationPolicy = paginationPolicy;
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

    public PaginatedResult<AuditLogView> list(String action, String targetType, Integer page, Integer size) {
        PaginationPolicy.PaginationSpec spec = paginationPolicy.page(page, size);
        String normalizedAction = action == null || action.isBlank() ? null : action.trim();
        String normalizedTargetType = targetType == null || targetType.isBlank() ? null : targetType.trim();

        PaginatedResult<AuditLog> result = auditLogPort.list(normalizedAction, normalizedTargetType, spec.page(), spec.size());
        return new PaginatedResult<>(
                result.items().stream().map(this::toView).toList(),
                result.page(),
                result.size(),
                result.totalElements(),
                result.totalPages()
        );
    }

    private AuditLogView toView(AuditLog auditLog) {
        return new AuditLogView(
                auditLog.getId(),
                auditLog.getActorUserId(),
                auditLog.getAction(),
                auditLog.getTargetType(),
                auditLog.getTargetId(),
                auditLog.getOldValue(),
                auditLog.getNewValue(),
                auditLog.getCreatedAt()
        );
    }
}
