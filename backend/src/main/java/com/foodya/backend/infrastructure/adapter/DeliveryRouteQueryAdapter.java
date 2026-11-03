package com.foodya.backend.infrastructure.adapter;

import com.foodya.backend.application.ports.out.DeliveryRouteQueryPort;
import com.foodya.backend.infrastructure.repository.DeliveryRouteRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Component
public class DeliveryRouteQueryAdapter implements DeliveryRouteQueryPort {

    private final DeliveryRouteRepository repository;

    public DeliveryRouteQueryAdapter(DeliveryRouteRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PlannedRoute> findActiveRouteForOrder(UUID orderId) {
        return repository.findFirstByOrderIdAndActiveTrueOrderByCalculatedAtDesc(orderId)
                .map(m -> new PlannedRoute(m.getEncodedPolyline(), m.getDistanceMeters(), m.getDurationSeconds()));
    }
}
