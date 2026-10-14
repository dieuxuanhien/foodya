package com.foodya.backend.interfaces.rest.dto;

import com.foodya.backend.domain.value_objects.BatchStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record BatchApiResponse(
        UUID batchId,
        UUID driverUserId,
        BatchStatus status,
        String pickupSequence,
        String deliverySequence,
        Integer totalDistanceMeters,
        Integer totalDurationSeconds,
        Instant createdAt,
        Instant startedAt,
        Instant completedAt,
        List<BatchItemApiResponse> items
) {}
