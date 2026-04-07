package com.foodya.backend.interfaces.rest.mapper;

import com.foodya.backend.application.dto.UserAccountData;
import com.foodya.backend.application.dto.MenuCategoryData;
import com.foodya.backend.application.dto.MenuItemData;
import com.foodya.backend.application.dto.RestaurantData;
import com.foodya.backend.application.dto.SystemParameterData;
import com.foodya.backend.interfaces.rest.dto.ProfileResponse;
import com.foodya.backend.interfaces.rest.dto.MenuCategoryResponse;
import com.foodya.backend.interfaces.rest.dto.MenuItemResponse;
import com.foodya.backend.interfaces.rest.dto.RestaurantDetailResponse;
import com.foodya.backend.interfaces.rest.dto.SystemParameterResponse;

public final class CommonApiMapper {

    private CommonApiMapper() {
    }

    public static ProfileResponse toProfileResponse(UserAccountData user) {
        return new ProfileResponse(
                user.getId().toString(),
                user.getUsername(),
                user.getEmail(),
                user.getPhoneNumber(),
                user.getFullName(),
                user.getAvatarUrl(),
                user.getRole(),
                user.getStatus()
        );
    }

    public static MenuCategoryResponse toMenuCategoryResponse(MenuCategoryData category) {
        return new MenuCategoryResponse(
                category.getId().toString(),
                category.getRestaurantId().toString(),
                category.getName(),
                category.getSortOrder(),
                category.isActive()
        );
    }

    public static MenuItemResponse toMenuItemResponse(MenuItemData item) {
        return new MenuItemResponse(
                item.getId().toString(),
                item.getRestaurantId().toString(),
                item.getCategoryId().toString(),
                item.getName(),
                item.getDescription(),
                item.getImageUrl(),
                item.getPrice(),
                item.isActive(),
                item.isAvailable()
        );
    }

    public static RestaurantDetailResponse toRestaurantDetailResponse(RestaurantData restaurant) {
        return new RestaurantDetailResponse(
                restaurant.getId().toString(),
                restaurant.getName(),
                restaurant.getCuisineType(),
                restaurant.getDescription(),
                restaurant.getAddressLine(),
                restaurant.getLatitude(),
                restaurant.getLongitude(),
                restaurant.getH3IndexRes9(),
                restaurant.getAvgRating(),
                restaurant.getReviewCount(),
                restaurant.getStatus(),
                restaurant.isOpen(),
                restaurant.getMaxDeliveryKm()
        );
    }

    public static SystemParameterResponse toSystemParameterResponse(SystemParameterData parameter) {
        return new SystemParameterResponse(
                parameter.getKey(),
                parameter.getValueType(),
                parameter.getValue(),
                parameter.isRuntimeApplicable(),
                parameter.getVersion(),
                parameter.getDescription(),
                parameter.getUpdatedByActor(),
                parameter.getUpdatedAt()
        );
    }
}
