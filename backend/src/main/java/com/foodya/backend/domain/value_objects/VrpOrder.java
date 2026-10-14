package com.foodya.backend.domain.value_objects;

import java.util.UUID;

/**
 * Represents an order in the VRP model with pickup/delivery node indices.
 */
public record VrpOrder(
        UUID orderId,
        int pickupNodeIndex,
        int deliveryNodeIndex,
        long timeWindowStart,
        long timeWindowEnd,
        long pickupServiceTime,
        long deliveryServiceTime
) {}
