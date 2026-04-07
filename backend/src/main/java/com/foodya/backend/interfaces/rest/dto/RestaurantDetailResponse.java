package com.foodya.backend.interfaces.rest.dto;

import com.foodya.backend.domain.value_objects.RestaurantStatus;

import java.math.BigDecimal;
import java.util.List;

public record RestaurantDetailResponse(
        String id,
        String name,
        String cuisineType,
        List<String> cuisineTypes,
        String description,
        String imageUrl,
        List<String> images,
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
