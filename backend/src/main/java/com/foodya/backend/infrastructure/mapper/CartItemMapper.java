package com.foodya.backend.infrastructure.mapper;

import com.foodya.backend.domain.entities.CartItem;
import com.foodya.backend.infrastructure.persistence.models.CartItemPersistenceModel;
import org.springframework.stereotype.Component;

@Component
public class CartItemMapper {

    public CartItem toDomain(CartItemPersistenceModel model) {
        if (model == null) {
            return null;
        }

        CartItem domain = new CartItem();
        domain.setId(model.getId());
        domain.setCartId(model.getCartId());
        domain.setMenuItemId(model.getMenuItemId());
        domain.setQuantity(model.getQuantity());
        domain.setUnitPriceSnapshot(model.getUnitPriceSnapshot());
        domain.setNote(model.getNote());
        return domain;
    }

    public CartItemPersistenceModel toPersistence(CartItem domain) {
        if (domain == null) {
            return null;
        }

        CartItemPersistenceModel model = new CartItemPersistenceModel();
        model.setId(domain.getId());
        model.setCartId(domain.getCartId());
        model.setMenuItemId(domain.getMenuItemId());
        model.setQuantity(domain.getQuantity());
        model.setUnitPriceSnapshot(domain.getUnitPriceSnapshot());
        model.setNote(domain.getNote());
        return model;
    }
}
