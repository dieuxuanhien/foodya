package com.foodya.backend.application.dto;

import com.foodya.backend.domain.value_objects.RestaurantStatus;

import java.math.BigDecimal;
import java.util.UUID;

public record AdminRestaurantCreateCommand(
        UUID ownerUserId,
        String name,
        String description,
        String addressLine,
        String cuisineType,
        boolean isOpen,
        RestaurantStatus status,
        BigDecimal latitude,
        BigDecimal longitude,
        BigDecimal maxDeliveryKm
) {
}
