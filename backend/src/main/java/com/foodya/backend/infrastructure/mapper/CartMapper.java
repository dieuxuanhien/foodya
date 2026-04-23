package com.foodya.backend.infrastructure.mapper;

import com.foodya.backend.domain.entities.Cart;
import com.foodya.backend.infrastructure.persistence.models.CartPersistenceModel;
import org.springframework.stereotype.Component;

@Component
public class CartMapper {

    public Cart toDomain(CartPersistenceModel model) {
        if (model == null) {
            return null;
        }

        Cart domain = new Cart();
        domain.setId(model.getId());
        domain.setCustomerUserId(model.getCustomerUserId());
        domain.setRestaurantId(model.getRestaurantId());
        domain.setStatus(model.getStatus());
        domain.setCreatedAt(model.getCreatedAt());
        domain.setUpdatedAt(model.getUpdatedAt());
        return domain;
    }

    public CartPersistenceModel toPersistence(Cart domain) {
        if (domain == null) {
            return null;
        }

        CartPersistenceModel model = new CartPersistenceModel();
        model.setId(domain.getId());
        model.setCustomerUserId(domain.getCustomerUserId());
        model.setRestaurantId(domain.getRestaurantId());
        model.setStatus(domain.getStatus());
        model.setCreatedAt(domain.getCreatedAt());
        model.setUpdatedAt(domain.getUpdatedAt());
        return model;
    }
}
