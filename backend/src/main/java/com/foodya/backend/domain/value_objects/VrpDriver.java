package com.foodya.backend.domain.value_objects;

import java.util.UUID;

/**
 * Represents a driver (vehicle) in the VRP model.
 */
public record VrpDriver(
        UUID driverUserId,
        UUID sessionId,
        int depotNodeIndex,
        int capacity
) {}
