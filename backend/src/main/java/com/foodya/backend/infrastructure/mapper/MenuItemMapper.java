package com.foodya.backend.infrastructure.mapper;

import com.foodya.backend.domain.entities.MenuItem;
import com.foodya.backend.infrastructure.persistence.models.MenuItemPersistenceModel;
import org.springframework.stereotype.Component;

@Component
public class MenuItemMapper {

    public MenuItem toDomain(MenuItemPersistenceModel model) {
        if (model == null) {
            return null;
        }

        MenuItem domain = new MenuItem();
        domain.setId(model.getId());
        domain.setRestaurantId(model.getRestaurantId());
        domain.setCategoryId(model.getCategoryId());
        domain.setName(model.getName());
        domain.setDescription(model.getDescription());
        domain.setPrice(model.getPrice());
        domain.setActive(model.isActive());
        domain.setAvailable(model.isAvailable());
        domain.setDeletedAt(model.getDeletedAt());
        domain.setCreatedAt(model.getCreatedAt());
        domain.setUpdatedAt(model.getUpdatedAt());
        return domain;
    }

    public MenuItemPersistenceModel toPersistence(MenuItem domain) {
        if (domain == null) {
            return null;
        }

        MenuItemPersistenceModel model = new MenuItemPersistenceModel();
        model.setId(domain.getId());
        model.setRestaurantId(domain.getRestaurantId());
        model.setCategoryId(domain.getCategoryId());
        model.setName(domain.getName());
        model.setDescription(domain.getDescription());
        model.setPrice(domain.getPrice());
        model.setActive(domain.isActive());
        model.setAvailable(domain.isAvailable());
        model.setDeletedAt(domain.getDeletedAt());
        model.setCreatedAt(domain.getCreatedAt());
        model.setUpdatedAt(domain.getUpdatedAt());
        return model;
    }
}
