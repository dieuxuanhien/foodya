package com.foodya.backend.application.ports.out;

import com.foodya.backend.domain.entities.DeliveryRoute;

import java.util.Optional;
import java.util.UUID;

public interface DeliveryRoutePort {

    DeliveryRoute save(DeliveryRoute route);

    /** Marks any active routes of the order as inactive. */
    void deactivateByOrderId(UUID orderId);

    Optional<DeliveryRoute> findActiveByOrderId(UUID orderId);
}
