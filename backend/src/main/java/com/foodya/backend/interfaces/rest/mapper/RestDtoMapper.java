package com.foodya.backend.interfaces.rest.mapper;

import com.foodya.backend.application.dto.UserAccountModel;
import com.foodya.backend.application.dto.MenuCategoryModel;
import com.foodya.backend.application.dto.MenuItemModel;
import com.foodya.backend.application.dto.RestaurantModel;
import com.foodya.backend.application.dto.SystemParameterModel;
import com.foodya.backend.interfaces.rest.dto.MeResponse;
import com.foodya.backend.interfaces.rest.dto.MenuCategoryResponse;
import com.foodya.backend.interfaces.rest.dto.MenuItemResponse;
import com.foodya.backend.interfaces.rest.dto.RestaurantDetailResponse;
import com.foodya.backend.interfaces.rest.dto.SystemParameterResponse;

public final class RestDtoMapper {

    private RestDtoMapper() {
    }

    public static MeResponse toMeResponse(UserAccountModel user) {
        return new MeResponse(
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

    public static MenuCategoryResponse toMenuCategoryResponse(MenuCategoryModel category) {
        return new MenuCategoryResponse(
                category.getId().toString(),
                category.getRestaurantId().toString(),
                category.getName(),
                category.getSortOrder(),
                category.isActive()
        );
    }

    public static MenuItemResponse toMenuItemResponse(MenuItemModel item) {
        return new MenuItemResponse(
                item.getId().toString(),
                item.getRestaurantId().toString(),
                item.getCategoryId().toString(),
                item.getName(),
                item.getDescription(),
                item.getPrice(),
                item.isActive(),
                item.isAvailable()
        );
    }

    public static RestaurantDetailResponse toRestaurantDetailResponse(RestaurantModel restaurant) {
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

    public static SystemParameterResponse toSystemParameterResponse(SystemParameterModel parameter) {
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
