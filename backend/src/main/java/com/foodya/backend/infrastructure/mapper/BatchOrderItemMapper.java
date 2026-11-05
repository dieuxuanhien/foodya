package com.foodya.backend.infrastructure.mapper;

import com.foodya.backend.domain.entities.BatchOrderItem;
import com.foodya.backend.infrastructure.persistence.models.BatchOrderItemPersistenceModel;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Component
public class BatchOrderItemMapper {

    public BatchOrderItem toDomain(BatchOrderItemPersistenceModel model) {
        if (model == null) return null;
        BatchOrderItem domain = new BatchOrderItem();
        domain.setId(model.getId());
        domain.setBatchId(model.getBatchId());
        domain.setOrderId(model.getOrderId());
        domain.setPickupIndex(model.getPickupIndex());
        domain.setDeliveryIndex(model.getDeliveryIndex());
        domain.setEstimatedPickupAt(model.getEstimatedPickupAt() != null ? model.getEstimatedPickupAt().toInstant() : null);
        domain.setEstimatedDeliveryAt(model.getEstimatedDeliveryAt() != null ? model.getEstimatedDeliveryAt().toInstant() : null);
        domain.setActualPickupAt(model.getActualPickupAt() != null ? model.getActualPickupAt().toInstant() : null);
        domain.setActualDeliveryAt(model.getActualDeliveryAt() != null ? model.getActualDeliveryAt().toInstant() : null);
        domain.setItemStatus(model.getItemStatus());
        domain.setCreatedAt(model.getCreatedAt() != null ? model.getCreatedAt().toInstant() : null);
        return domain;
    }

    public BatchOrderItemPersistenceModel toPersistence(BatchOrderItem domain) {
        if (domain == null) return null;
        BatchOrderItemPersistenceModel model = new BatchOrderItemPersistenceModel();
        model.setId(domain.getId());
        model.setBatchId(domain.getBatchId());
        model.setOrderId(domain.getOrderId());
        model.setPickupIndex(domain.getPickupIndex());
        model.setDeliveryIndex(domain.getDeliveryIndex());
        model.setEstimatedPickupAt(domain.getEstimatedPickupAt() != null ? OffsetDateTime.ofInstant(domain.getEstimatedPickupAt(), ZoneOffset.UTC) : null);
        model.setEstimatedDeliveryAt(domain.getEstimatedDeliveryAt() != null ? OffsetDateTime.ofInstant(domain.getEstimatedDeliveryAt(), ZoneOffset.UTC) : null);
        model.setActualPickupAt(domain.getActualPickupAt() != null ? OffsetDateTime.ofInstant(domain.getActualPickupAt(), ZoneOffset.UTC) : null);
        model.setActualDeliveryAt(domain.getActualDeliveryAt() != null ? OffsetDateTime.ofInstant(domain.getActualDeliveryAt(), ZoneOffset.UTC) : null);
        model.setItemStatus(domain.getItemStatus());
        model.setCreatedAt(domain.getCreatedAt() != null ? OffsetDateTime.ofInstant(domain.getCreatedAt(), ZoneOffset.UTC) : null);
        return model;
    }
}
