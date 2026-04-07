package com.foodya.backend.application.dto;


import java.math.BigDecimal;
import java.util.List;

public record UpdateRestaurantRequest(
        String name,
        String cuisineType,
        List<String> cuisineTypes,
        String description,
        String addressLine,
        BigDecimal latitude,
        BigDecimal longitude,
        BigDecimal maxDeliveryKm,
        Boolean isOpen
) {
}
