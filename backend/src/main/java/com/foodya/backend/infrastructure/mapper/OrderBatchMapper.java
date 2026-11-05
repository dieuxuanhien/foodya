package com.foodya.backend.infrastructure.mapper;

import com.foodya.backend.domain.entities.OrderBatch;
import com.foodya.backend.infrastructure.persistence.models.OrderBatchPersistenceModel;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Component
public class OrderBatchMapper {

    public OrderBatch toDomain(OrderBatchPersistenceModel model) {
        if (model == null) return null;
        OrderBatch domain = new OrderBatch();
        domain.setId(model.getId());
        domain.setDriverUserId(model.getDriverUserId());
        domain.setSessionId(model.getSessionId());
        domain.setStatus(model.getStatus());
        domain.setPickupSequence(model.getPickupSequence());
        domain.setDeliverySequence(model.getDeliverySequence());
        domain.setTotalDistanceMeters(model.getTotalDistanceMeters());
        domain.setTotalDurationSeconds(model.getTotalDurationSeconds());
        domain.setOptimizedRoutePolyline(model.getOptimizedRoutePolyline());
        domain.setSolverMetadata(model.getSolverMetadata());
        domain.setCreatedAt(model.getCreatedAt() != null ? model.getCreatedAt().toInstant() : null);
        domain.setStartedAt(model.getStartedAt() != null ? model.getStartedAt().toInstant() : null);
        domain.setCompletedAt(model.getCompletedAt() != null ? model.getCompletedAt().toInstant() : null);
        return domain;
    }

    public OrderBatchPersistenceModel toPersistence(OrderBatch domain) {
        if (domain == null) return null;
        OrderBatchPersistenceModel model = new OrderBatchPersistenceModel();
        model.setId(domain.getId());
        model.setDriverUserId(domain.getDriverUserId());
        model.setSessionId(domain.getSessionId());
        model.setStatus(domain.getStatus());
        model.setPickupSequence(domain.getPickupSequence());
        model.setDeliverySequence(domain.getDeliverySequence());
        model.setTotalDistanceMeters(domain.getTotalDistanceMeters());
        model.setTotalDurationSeconds(domain.getTotalDurationSeconds());
        model.setOptimizedRoutePolyline(domain.getOptimizedRoutePolyline());
        model.setSolverMetadata(domain.getSolverMetadata());
        model.setCreatedAt(domain.getCreatedAt() != null ? OffsetDateTime.ofInstant(domain.getCreatedAt(), ZoneOffset.UTC) : null);
        model.setStartedAt(domain.getStartedAt() != null ? OffsetDateTime.ofInstant(domain.getStartedAt(), ZoneOffset.UTC) : null);
        model.setCompletedAt(domain.getCompletedAt() != null ? OffsetDateTime.ofInstant(domain.getCompletedAt(), ZoneOffset.UTC) : null);
        return model;
    }
}
