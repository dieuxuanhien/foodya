package com.foodya.backend.domain.value_objects;

import java.util.UUID;

public record RouteWaypoint(double lat, double lng, String type, UUID orderId) {
    public static final String PICKUP = "PICKUP";
    public static final String DROPOFF = "DROPOFF";
    public static final String DRIVER = "DRIVER";
}
