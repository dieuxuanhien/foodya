package com.foodya.backend.application.ports.in;

import com.foodya.backend.domain.entities.DeliveryRoute;
import com.foodya.backend.domain.value_objects.RouteWaypoint;

import java.util.List;
import java.util.UUID;

public interface DeliveryRouteUseCase {

    /** Plans driver → restaurant → customer for a single assigned order and stores it. */
    DeliveryRoute planOrderRoute(UUID orderId, UUID driverUserId);

    /**
     * Plans a route visiting the given stops (already in solver order) and stores one route row per
     * distinct order. Stops carry type PICKUP/DROPOFF and the owning orderId.
     */
    List<DeliveryRoute> planBatchRoute(UUID batchId, UUID driverUserId, List<RouteWaypoint> stops);

    DeliveryRoute getRouteForCustomer(UUID orderId, UUID customerUserId);

    DeliveryRoute getRouteForDriver(UUID orderId, UUID driverUserId);
}
