package com.foodya.backend.infrastructure.mapper;

import com.foodya.backend.application.dto.MenuItemModel;
import com.foodya.backend.application.dto.RestaurantModel;
import com.foodya.backend.domain.entities.MenuItem;
import com.foodya.backend.domain.entities.Restaurant;

public final class CatalogPersistenceMapper {

    private CatalogPersistenceMapper() {
    }

    public static RestaurantModel toModel(Restaurant entity) {
        RestaurantModel model = new RestaurantModel();
        model.setId(entity.getId());
        model.setOwnerUserId(entity.getOwnerUserId());
        model.setName(entity.getName());
        model.setCuisineType(entity.getCuisineType());
        model.setDescription(entity.getDescription());
        model.setAddressLine(entity.getAddressLine());
        model.setLatitude(entity.getLatitude());
        model.setLongitude(entity.getLongitude());
        model.setH3IndexRes9(entity.getH3IndexRes9());
        model.setAvgRating(entity.getAvgRating());
        model.setReviewCount(entity.getReviewCount());
        model.setStatus(entity.getStatus());
        model.setOpen(entity.isOpen());
        model.setMaxDeliveryKm(entity.getMaxDeliveryKm());
        return model;
    }

    public static MenuItemModel toModel(MenuItem entity) {
        MenuItemModel model = new MenuItemModel();
        model.setId(entity.getId());
        model.setRestaurantId(entity.getRestaurantId());
        model.setCategoryId(entity.getCategoryId());
        model.setName(entity.getName());
        model.setDescription(entity.getDescription());
        model.setPrice(entity.getPrice());
        model.setActive(entity.isActive());
        model.setAvailable(entity.isAvailable());
        model.setDeletedAt(entity.getDeletedAt());
        return model;
    }
}