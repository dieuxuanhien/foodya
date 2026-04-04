package com.foodya.backend.infrastructure.mapper;

import com.foodya.backend.domain.entities.AuditLog;
import com.foodya.backend.infrastructure.persistence.models.AuditLogPersistenceModel;
import org.springframework.stereotype.Component;

/**
 * MAPPER: AuditLog Domain Entity ↔ AuditLogPersistenceModel
 */
@Component
public class AuditLogMapper {

    public AuditLog toDomain(AuditLogPersistenceModel model) {
        if (model == null) {
            return null;
        }

        AuditLog domain = new AuditLog();
        domain.setId(model.getId());
        domain.setActorUserId(model.getActorUserId());
        domain.setAction(model.getAction());
        domain.setTargetType(model.getTargetType());
        domain.setTargetId(model.getTargetId());
        domain.setOldValue(model.getOldValue());
        domain.setNewValue(model.getNewValue());
        domain.setCreatedAt(model.getCreatedAt());

        return domain;
    }

    public AuditLogPersistenceModel toPersistence(AuditLog domain) {
        if (domain == null) {
            return null;
        }

        AuditLogPersistenceModel model = new AuditLogPersistenceModel();
        model.setId(domain.getId());
        model.setActorUserId(domain.getActorUserId());
        model.setAction(domain.getAction());
        model.setTargetType(domain.getTargetType());
        model.setTargetId(domain.getTargetId());
        model.setOldValue(domain.getOldValue());
        model.setNewValue(domain.getNewValue());
        model.setCreatedAt(domain.getCreatedAt());

        return model;
    }
}
