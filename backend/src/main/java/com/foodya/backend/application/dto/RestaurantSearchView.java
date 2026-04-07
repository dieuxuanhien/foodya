package com.foodya.backend.application.dto;

import java.math.BigDecimal;
import java.util.List;

public record RestaurantSearchView(
        String restaurantId,
        String restaurantName,
        String cuisine,
        String backgroundImageUrl,
        String avatarImageUrl,
        BigDecimal rating,
        boolean openStatus,
        BigDecimal maxDeliveryKm,
        BigDecimal distanceKm,
        List<MatchedMenuItemView> matchedItems
) {
        public static RestaurantSearchView from(RestaurantData restaurant,
                                                BigDecimal distanceKm,
                                                List<MatchedMenuItemView> matchedItems) {
        return new RestaurantSearchView(
                restaurant.getId().toString(),
                restaurant.getName(),
                restaurant.getCuisineType(),
                restaurant.getBackgroundImageUrl(),
                restaurant.getAvatarImageUrl(),
                restaurant.getAvgRating(),
                restaurant.isOpen(),
                restaurant.getMaxDeliveryKm(),
                distanceKm,
                matchedItems
        );
    }
}
