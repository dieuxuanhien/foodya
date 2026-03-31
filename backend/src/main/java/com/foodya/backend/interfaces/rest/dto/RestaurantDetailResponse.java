package com.foodya.backend.interfaces.rest.dto;

import com.foodya.backend.domain.model.RestaurantStatus;

import java.math.BigDecimal;

public record RestaurantDetailResponse(
        String id,
        String name,
        String cuisineType,
        String description,
        String addressLine,
        BigDecimal latitude,
        BigDecimal longitude,
        String h3IndexRes9,
        BigDecimal avgRating,
        int reviewCount,
        RestaurantStatus status,
        boolean open,
        BigDecimal maxDeliveryKm
) {
}
