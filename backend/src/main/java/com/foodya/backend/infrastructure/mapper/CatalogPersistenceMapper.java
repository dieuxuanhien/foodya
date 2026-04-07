package com.foodya.backend.infrastructure.mapper;

import com.foodya.backend.application.dto.MenuItemData;
import com.foodya.backend.application.dto.RestaurantData;
import com.foodya.backend.domain.entities.MenuItem;
import com.foodya.backend.domain.entities.Restaurant;

public final class CatalogPersistenceMapper {

    private CatalogPersistenceMapper() {
    }

    public static RestaurantData toData(Restaurant entity) {
        RestaurantData data = new RestaurantData();
        data.setId(entity.getId());
        data.setOwnerUserId(entity.getOwnerUserId());
        data.setName(entity.getName());
        data.setCuisineType(entity.getCuisineType());
        data.setCuisineTypes(entity.getCuisineTypes());
        data.setDescription(entity.getDescription());
        data.setBackgroundImageUrl(entity.getBackgroundImageUrl() != null ? entity.getBackgroundImageUrl() : entity.getImageUrl());
        data.setAvatarImageUrl(entity.getAvatarImageUrl());
        data.setAddressLine(entity.getAddressLine());
        data.setLatitude(entity.getLatitude());
        data.setLongitude(entity.getLongitude());
        data.setH3IndexRes9(entity.getH3IndexRes9());
        data.setAvgRating(entity.getAvgRating());
        data.setReviewCount(entity.getReviewCount());
        data.setStatus(entity.getStatus());
        data.setOpen(entity.isOpen());
        data.setMaxDeliveryKm(entity.getMaxDeliveryKm());
        return data;
    }

    public static MenuItemData toData(MenuItem entity) {
        MenuItemData data = new MenuItemData();
        data.setId(entity.getId());
        data.setRestaurantId(entity.getRestaurantId());
        data.setCategoryId(entity.getCategoryId());
        data.setName(entity.getName());
        data.setDescription(entity.getDescription());
        data.setImageUrl(entity.getImageUrl());
        data.setPrice(entity.getPrice());
        data.setActive(entity.isActive());
        data.setAvailable(entity.isAvailable());
        data.setDeletedAt(entity.getDeletedAt());
        return data;
    }
}