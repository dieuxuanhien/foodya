package com.foodya.backend.infrastructure.mapper;

import com.foodya.backend.domain.entities.DeliveryTrackingPoint;
import com.foodya.backend.infrastructure.persistence.models.DeliveryTrackingPointPersistenceModel;
import org.springframework.stereotype.Component;

@Component
public class DeliveryTrackingPointMapper {

    public DeliveryTrackingPoint toDomain(DeliveryTrackingPointPersistenceModel model) {
        if (model == null) {
            return null;
        }

        DeliveryTrackingPoint domain = new DeliveryTrackingPoint();
        domain.setId(model.getId());
        domain.setOrderId(model.getOrderId());
        domain.setLat(model.getLat());
        domain.setLng(model.getLng());
        domain.setRecordedAt(model.getRecordedAt());
        domain.setCreatedAt(model.getCreatedAt());
        return domain;
    }

    public DeliveryTrackingPointPersistenceModel toPersistence(DeliveryTrackingPoint domain) {
        if (domain == null) {
            return null;
        }

        DeliveryTrackingPointPersistenceModel model = new DeliveryTrackingPointPersistenceModel();
        model.setId(domain.getId());
        model.setOrderId(domain.getOrderId());
        model.setLat(domain.getLat());
        model.setLng(domain.getLng());
        model.setRecordedAt(domain.getRecordedAt());
        model.setCreatedAt(domain.getCreatedAt());
        return model;
    }
}
