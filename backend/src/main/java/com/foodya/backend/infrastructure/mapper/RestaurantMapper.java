package com.foodya.backend.infrastructure.mapper;

import com.foodya.backend.domain.entities.Restaurant;
import com.foodya.backend.infrastructure.persistence.models.RestaurantPersistenceModel;
import org.springframework.stereotype.Component;

@Component
public class RestaurantMapper {

    public Restaurant toDomain(RestaurantPersistenceModel model) {
        if (model == null) {
            return null;
        }

        Restaurant domain = new Restaurant();
        domain.setId(model.getId());
        domain.setOwnerUserId(model.getOwnerUserId());
        domain.setName(model.getName());
        domain.setCuisineType(model.getCuisineType());
        domain.setDescription(model.getDescription());
        domain.setAddressLine(model.getAddressLine());
        domain.setLatitude(model.getLatitude());
        domain.setLongitude(model.getLongitude());
        domain.setH3IndexRes9(model.getH3IndexRes9());
        domain.setAvgRating(model.getAvgRating());
        domain.setReviewCount(model.getReviewCount());
        domain.setStatus(model.getStatus());
        domain.setOpen(model.isOpen());
        domain.setMaxDeliveryKm(model.getMaxDeliveryKm());
        domain.setCreatedAt(model.getCreatedAt());
        domain.setUpdatedAt(model.getUpdatedAt());
        return domain;
    }

    public RestaurantPersistenceModel toPersistence(Restaurant domain) {
        if (domain == null) {
            return null;
        }

        RestaurantPersistenceModel model = new RestaurantPersistenceModel();
        model.setId(domain.getId());
        model.setOwnerUserId(domain.getOwnerUserId());
        model.setName(domain.getName());
        model.setCuisineType(domain.getCuisineType());
        model.setDescription(domain.getDescription());
        model.setAddressLine(domain.getAddressLine());
        model.setLatitude(domain.getLatitude());
        model.setLongitude(domain.getLongitude());
        model.setH3IndexRes9(domain.getH3IndexRes9());
        model.setAvgRating(domain.getAvgRating());
        model.setReviewCount(domain.getReviewCount());
        model.setStatus(domain.getStatus());
        model.setOpen(domain.isOpen());
        model.setMaxDeliveryKm(domain.getMaxDeliveryKm());
        model.setCreatedAt(domain.getCreatedAt());
        model.setUpdatedAt(domain.getUpdatedAt());
        return model;
    }
}
