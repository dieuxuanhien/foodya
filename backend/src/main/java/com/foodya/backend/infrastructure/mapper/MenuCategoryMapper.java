package com.foodya.backend.infrastructure.mapper;

import com.foodya.backend.domain.entities.MenuCategory;
import com.foodya.backend.infrastructure.persistence.models.MenuCategoryPersistenceModel;
import org.springframework.stereotype.Component;

@Component
public class MenuCategoryMapper {

    public MenuCategory toDomain(MenuCategoryPersistenceModel model) {
        if (model == null) {
            return null;
        }

        MenuCategory domain = new MenuCategory();
        domain.setId(model.getId());
        domain.setRestaurantId(model.getRestaurantId());
        domain.setName(model.getName());
        domain.setSortOrder(model.getSortOrder());
        domain.setActive(model.isActive());
        domain.setCreatedAt(model.getCreatedAt());
        domain.setUpdatedAt(model.getUpdatedAt());
        return domain;
    }

    public MenuCategoryPersistenceModel toPersistence(MenuCategory domain) {
        if (domain == null) {
            return null;
        }

        MenuCategoryPersistenceModel model = new MenuCategoryPersistenceModel();
        model.setId(domain.getId());
        model.setRestaurantId(domain.getRestaurantId());
        model.setName(domain.getName());
        model.setSortOrder(domain.getSortOrder());
        model.setActive(domain.isActive());
        model.setCreatedAt(domain.getCreatedAt());
        model.setUpdatedAt(domain.getUpdatedAt());
        return model;
    }
}
