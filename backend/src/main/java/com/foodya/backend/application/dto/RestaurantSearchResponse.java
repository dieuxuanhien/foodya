package com.foodya.backend.application.dto;

import java.math.BigDecimal;
import java.util.List;

public record RestaurantSearchResponse(
        String restaurantId,
        String restaurantName,
        String cuisine,
        BigDecimal rating,
        boolean openStatus,
        BigDecimal maxDeliveryKm,
        BigDecimal distanceKm,
        List<MatchedMenuItemResponse> matchedItems
) {
        public static RestaurantSearchResponse from(RestaurantModel restaurant,
                                                BigDecimal distanceKm,
                                                List<MatchedMenuItemResponse> matchedItems) {
        return new RestaurantSearchResponse(
                restaurant.getId().toString(),
                restaurant.getName(),
                restaurant.getCuisineType(),
                restaurant.getAvgRating(),
                restaurant.isOpen(),
                restaurant.getMaxDeliveryKm(),
                distanceKm,
                matchedItems
        );
    }
}
