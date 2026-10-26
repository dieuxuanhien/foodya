package com.foodya.backend.infrastructure.adapter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.foodya.backend.application.ports.out.DeliveryRoutePort;
import com.foodya.backend.domain.entities.DeliveryRoute;
import com.foodya.backend.domain.value_objects.RouteWaypoint;
import com.foodya.backend.infrastructure.persistence.models.DeliveryRoutePersistenceModel;
import com.foodya.backend.infrastructure.repository.DeliveryRouteRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class DeliveryRoutePersistenceAdapter implements DeliveryRoutePort {

    private final DeliveryRouteRepository repository;
    private final ObjectMapper objectMapper;

    public DeliveryRoutePersistenceAdapter(DeliveryRouteRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public DeliveryRoute save(DeliveryRoute route) {
        return toDomain(repository.save(toModel(route)));
    }

    @Override
    @Transactional
    public void deactivateByOrderId(UUID orderId) {
        repository.deactivateByOrderId(orderId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<DeliveryRoute> findActiveByOrderId(UUID orderId) {
        return repository.findFirstByOrderIdAndActiveTrueOrderByCalculatedAtDesc(orderId).map(this::toDomain);
    }

    private DeliveryRoutePersistenceModel toModel(DeliveryRoute d) {
        DeliveryRoutePersistenceModel m = new DeliveryRoutePersistenceModel();
        m.setId(d.getId());
        m.setOrderId(d.getOrderId());
        m.setBatchId(d.getBatchId());
        m.setEncodedPolyline(d.getEncodedPolyline());
        m.setDistanceMeters(d.getDistanceMeters());
        m.setDurationSeconds(d.getDurationSeconds());
        try {
            m.setWaypoints(d.getWaypoints() == null ? null : objectMapper.writeValueAsString(d.getWaypoints()));
        } catch (Exception ex) {
            throw new IllegalStateException("Cannot serialize route waypoints", ex);
        }
        m.setCalculatedAt(OffsetDateTime.ofInstant(d.getCalculatedAt(), ZoneOffset.UTC));
        m.setActive(d.isActive());
        return m;
    }

    private DeliveryRoute toDomain(DeliveryRoutePersistenceModel m) {
        DeliveryRoute d = new DeliveryRoute();
        d.setId(m.getId());
        d.setOrderId(m.getOrderId());
        d.setBatchId(m.getBatchId());
        d.setEncodedPolyline(m.getEncodedPolyline());
        d.setDistanceMeters(m.getDistanceMeters());
        d.setDurationSeconds(m.getDurationSeconds());
        try {
            d.setWaypoints(m.getWaypoints() == null ? List.of()
                    : objectMapper.readValue(m.getWaypoints(), new TypeReference<List<RouteWaypoint>>() {}));
        } catch (Exception ex) {
            throw new IllegalStateException("Cannot parse stored route waypoints", ex);
        }
        d.setCalculatedAt(m.getCalculatedAt().toInstant());
        d.setActive(m.isActive());
        return d;
    }
}
