package com.foodya.backend.infrastructure.mapper;

import com.foodya.backend.domain.entities.DriverOnlineSession;
import com.foodya.backend.domain.value_objects.DriverSessionStatus;
import com.foodya.backend.infrastructure.persistence.models.DriverOnlineSessionPersistenceModel;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * Maps between {@link DriverOnlineSession} domain entities and
 * {@link DriverOnlineSessionPersistenceModel} JPA entities.
 */
@Component
public class DriverOnlineSessionMapper {

    public DriverOnlineSession toDomain(DriverOnlineSessionPersistenceModel model) {
        if (model == null) return null;
        DriverOnlineSession session = new DriverOnlineSession();
        session.setId(model.getId());
        session.setDriverUserId(model.getDriverUserId());
        session.setStatus(DriverSessionStatus.valueOf(model.getStatus()));
        session.setStartedAt(model.getStartedAt() != null
                ? model.getStartedAt().toInstant() : null);
        session.setEndedAt(model.getEndedAt() != null
                ? model.getEndedAt().toInstant() : null);
        session.setLastHeartbeat(model.getLastHeartbeat() != null
                ? model.getLastHeartbeat().toInstant() : null);
        session.setH3IndexRes9(model.getH3IndexRes9());
        session.setCurrentLat(model.getCurrentLat());
        session.setCurrentLng(model.getCurrentLng());
        session.setActiveOrderCount(model.getActiveOrderCount());
        session.setMaxConcurrentOrders(model.getMaxConcurrentOrders());
        session.setCreatedAt(model.getCreatedAt() != null
                ? model.getCreatedAt().toInstant() : null);
        return session;
    }

    public DriverOnlineSessionPersistenceModel toPersistence(DriverOnlineSession session) {
        if (session == null) return null;
        DriverOnlineSessionPersistenceModel model = new DriverOnlineSessionPersistenceModel();
        model.setId(session.getId());
        model.setDriverUserId(session.getDriverUserId());
        model.setStatus(session.getStatus() != null ? session.getStatus().name() : null);
        model.setStartedAt(session.getStartedAt() != null
                ? OffsetDateTime.ofInstant(session.getStartedAt(), ZoneOffset.UTC) : null);
        model.setEndedAt(session.getEndedAt() != null
                ? OffsetDateTime.ofInstant(session.getEndedAt(), ZoneOffset.UTC) : null);
        model.setLastHeartbeat(session.getLastHeartbeat() != null
                ? OffsetDateTime.ofInstant(session.getLastHeartbeat(), ZoneOffset.UTC) : null);
        model.setH3IndexRes9(session.getH3IndexRes9());
        model.setCurrentLat(session.getCurrentLat());
        model.setCurrentLng(session.getCurrentLng());
        model.setActiveOrderCount(session.getActiveOrderCount());
        model.setMaxConcurrentOrders(session.getMaxConcurrentOrders());
        model.setCreatedAt(session.getCreatedAt() != null
                ? OffsetDateTime.ofInstant(session.getCreatedAt(), ZoneOffset.UTC) : null);
        return model;
    }
}
