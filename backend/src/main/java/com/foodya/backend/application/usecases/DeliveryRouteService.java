package com.foodya.backend.application.usecases;

import com.foodya.backend.application.exception.ForbiddenException;
import com.foodya.backend.application.exception.NotFoundException;
import com.foodya.backend.application.ports.in.DeliveryRouteUseCase;
import com.foodya.backend.application.ports.out.*;
import com.foodya.backend.domain.entities.DeliveryRoute;
import com.foodya.backend.domain.entities.Order;
import com.foodya.backend.domain.entities.OrderBatch;
import com.foodya.backend.domain.entities.Restaurant;
import com.foodya.backend.domain.value_objects.LatLng;
import com.foodya.backend.domain.value_objects.RouteResult;
import com.foodya.backend.domain.value_objects.RouteWaypoint;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;

/** Framework-free route planning/storage service. */
public class DeliveryRouteService implements DeliveryRouteUseCase {

    private final RouteCalculationPort routeCalculationPort;
    private final DeliveryRoutePort deliveryRoutePort;
    private final OrderManagementPort orderManagementPort;
    private final RestaurantPort restaurantPort;
    private final DriverOnlineSessionPort driverOnlineSessionPort;
    private final OrderBatchPort orderBatchPort;

    public DeliveryRouteService(RouteCalculationPort routeCalculationPort,
                                DeliveryRoutePort deliveryRoutePort,
                                OrderManagementPort orderManagementPort,
                                RestaurantPort restaurantPort,
                                DriverOnlineSessionPort driverOnlineSessionPort,
                                OrderBatchPort orderBatchPort) {
        this.routeCalculationPort = routeCalculationPort;
        this.deliveryRoutePort = deliveryRoutePort;
        this.orderManagementPort = orderManagementPort;
        this.restaurantPort = restaurantPort;
        this.driverOnlineSessionPort = driverOnlineSessionPort;
        this.orderBatchPort = orderBatchPort;
    }

    @Override
    public DeliveryRoute planOrderRoute(UUID orderId, UUID driverUserId) {
        Order order = orderManagementPort.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found"));
        Restaurant restaurant = restaurantPort.findById(order.getRestaurantId())
                .orElseThrow(() -> new NotFoundException("Restaurant not found"));
        if (restaurant.getLatitude() == null || restaurant.getLongitude() == null
                || order.getDeliveryLatitude() == null || order.getDeliveryLongitude() == null) {
            throw new IllegalStateException("Order or restaurant has no coordinates");
        }

        List<RouteWaypoint> stops = List.of(
                new RouteWaypoint(restaurant.getLatitude().doubleValue(),
                        restaurant.getLongitude().doubleValue(), RouteWaypoint.PICKUP, orderId),
                new RouteWaypoint(order.getDeliveryLatitude().doubleValue(),
                        order.getDeliveryLongitude().doubleValue(), RouteWaypoint.DROPOFF, orderId));

        List<RouteWaypoint> all = withDriverOrigin(driverUserId, stops);
        RouteResult result = calculate(all);
        deactivate(orderId);
        DeliveryRoute saved = deliveryRoutePort.save(DeliveryRoute.create(
                orderId, order.getBatchId(), result.encodedPolyline(),
                result.distanceMeters(), result.durationSeconds(), all));

        order.setEstimatedDeliveryAt(OffsetDateTime.now(ZoneOffset.UTC).plusSeconds(result.durationSeconds()));
        orderManagementPort.save(order);
        return saved;
    }

    @Override
    public List<DeliveryRoute> planBatchRoute(UUID batchId, UUID driverUserId, List<RouteWaypoint> stops) {
        if (stops == null || stops.isEmpty()) {
            return List.of();
        }
        List<RouteWaypoint> all = withDriverOrigin(driverUserId, stops);
        RouteResult result = calculate(all);

        Set<UUID> orderIds = new LinkedHashSet<>();
        for (RouteWaypoint s : stops) {
            if (s.orderId() != null) orderIds.add(s.orderId());
        }
        List<DeliveryRoute> saved = new ArrayList<>();
        for (UUID orderId : orderIds) {
            deactivate(orderId);
            saved.add(deliveryRoutePort.save(DeliveryRoute.create(
                    orderId, batchId, result.encodedPolyline(),
                    result.distanceMeters(), result.durationSeconds(), all)));
        }

        OrderBatch batch = orderBatchPort.findById(batchId).orElse(null);
        if (batch != null) {
            batch.setOptimizedRoutePolyline(result.encodedPolyline());
            orderBatchPort.save(batch);
        }
        return saved;
    }

    @Override
    public DeliveryRoute getRouteForCustomer(UUID orderId, UUID customerUserId) {
        Order order = orderManagementPort.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found"));
        if (!customerUserId.equals(order.getCustomerUserId())) {
            throw new ForbiddenException("Order does not belong to the customer");
        }
        return findRoute(orderId);
    }

    @Override
    public DeliveryRoute getRouteForDriver(UUID orderId, UUID driverUserId) {
        Order order = orderManagementPort.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found"));
        if (!driverUserId.equals(order.getDriverUserId())) {
            throw new ForbiddenException("Order is not assigned to the driver");
        }
        return findRoute(orderId);
    }

    private DeliveryRoute findRoute(UUID orderId) {
        return deliveryRoutePort.findActiveByOrderId(orderId)
                .orElseThrow(() -> new NotFoundException("No route calculated for this order"));
    }

    private void deactivate(UUID orderId) {
        deliveryRoutePort.deactivateByOrderId(orderId);
    }

    /** Prepends the driver's current position (when known) as the route origin. */
    private List<RouteWaypoint> withDriverOrigin(UUID driverUserId, List<RouteWaypoint> stops) {
        List<RouteWaypoint> all = new ArrayList<>();
        driverOnlineSessionPort.findActiveSession(driverUserId).ifPresent(s -> {
            if (s.getCurrentLat() != null && s.getCurrentLng() != null) {
                all.add(new RouteWaypoint(s.getCurrentLat(), s.getCurrentLng(), RouteWaypoint.DRIVER, null));
            }
        });
        all.addAll(stops);
        return all;
    }

    private RouteResult calculate(List<RouteWaypoint> points) {
        if (points.size() < 2) {
            throw new IllegalStateException("A route needs at least two points");
        }
        LatLng origin = toLatLng(points.get(0));
        LatLng destination = toLatLng(points.get(points.size() - 1));
        List<LatLng> middle = points.subList(1, points.size() - 1).stream()
                .map(DeliveryRouteService::toLatLng).toList();
        return routeCalculationPort.calculateRoute(origin, destination, middle);
    }

    private static LatLng toLatLng(RouteWaypoint w) {
        return new LatLng(w.lat(), w.lng());
    }
}
