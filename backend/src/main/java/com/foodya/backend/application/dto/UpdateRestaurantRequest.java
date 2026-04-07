package com.foodya.backend.application.dto;


import java.math.BigDecimal;

public record UpdateRestaurantRequest(
        String name,
        String cuisineType,
        String description,
        String addressLine,
        BigDecimal latitude,
        BigDecimal longitude,
        BigDecimal maxDeliveryKm,
        Boolean isOpen
) {
}
