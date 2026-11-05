package com.foodya.backend.interfaces.rest.dto;

import java.time.Instant;
import java.util.UUID;

public record CustomerBatchInfoApiResponse(
        UUID batchId,
        int totalOrdersInBatch,
        int myDeliveryPosition,
        Instant estimatedDeliveryAt,
        int estimatedMinutesRemaining
) {}
