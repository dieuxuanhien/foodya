package com.foodya.backend.interfaces.rest.dto;

import com.foodya.backend.domain.value_objects.BatchItemStatus;

import java.time.Instant;
import java.util.UUID;

public record BatchItemApiResponse(
        UUID id,
        UUID orderId,
        int pickupIndex,
        int deliveryIndex,
        Instant estimatedPickupAt,
        Instant estimatedDeliveryAt,
        Instant actualPickupAt,
        Instant actualDeliveryAt,
        BatchItemStatus itemStatus
) {}
