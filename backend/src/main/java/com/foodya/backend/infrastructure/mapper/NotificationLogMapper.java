package com.foodya.backend.infrastructure.mapper;

import com.foodya.backend.domain.entities.NotificationLog;
import com.foodya.backend.infrastructure.persistence.models.NotificationLogPersistenceModel;
import org.springframework.stereotype.Component;

@Component
public class NotificationLogMapper {

    public NotificationLog toDomain(NotificationLogPersistenceModel model) {
        if (model == null) {
            return null;
        }

        NotificationLog domain = new NotificationLog();
        domain.setId(model.getId());
        domain.setReceiverUserId(model.getReceiverUserId());
        domain.setReceiverType(model.getReceiverType());
        domain.setEventType(model.getEventType());
        domain.setTitle(model.getTitle());
        domain.setMessage(model.getMessage());
        domain.setStatus(model.getStatus());
        domain.setOrderId(model.getOrderId());
        domain.setProviderResponse(model.getProviderResponse());
        domain.setSentAt(model.getSentAt());
        domain.setReadAt(model.getReadAt());
        domain.setCreatedAt(model.getCreatedAt());
        return domain;
    }

    public NotificationLogPersistenceModel toPersistence(NotificationLog domain) {
        if (domain == null) {
            return null;
        }

        NotificationLogPersistenceModel model = new NotificationLogPersistenceModel();
        model.setId(domain.getId());
        model.setReceiverUserId(domain.getReceiverUserId());
        model.setReceiverType(domain.getReceiverType());
        model.setEventType(domain.getEventType());
        model.setTitle(domain.getTitle());
        model.setMessage(domain.getMessage());
        model.setStatus(domain.getStatus());
        model.setOrderId(domain.getOrderId());
        model.setProviderResponse(domain.getProviderResponse());
        model.setSentAt(domain.getSentAt());
        model.setReadAt(domain.getReadAt());
        model.setCreatedAt(domain.getCreatedAt());
        return model;
    }
}