package com.foodya.backend.infrastructure.mapper;

import com.foodya.backend.domain.entities.OrderItem;
import com.foodya.backend.infrastructure.persistence.models.OrderItemPersistenceModel;
import org.springframework.stereotype.Component;

@Component
public class OrderItemMapper {

    public OrderItem toDomain(OrderItemPersistenceModel model) {
        if (model == null) {
            return null;
        }

        OrderItem domain = new OrderItem();
        domain.setId(model.getId());
        domain.setOrderId(model.getOrderId());
        domain.setMenuItemId(model.getMenuItemId());
        domain.setMenuItemNameSnapshot(model.getMenuItemNameSnapshot());
        domain.setUnitPriceSnapshot(model.getUnitPriceSnapshot());
        domain.setQuantity(model.getQuantity());
        domain.setLineTotal(model.getLineTotal());
        return domain;
    }

    public OrderItemPersistenceModel toPersistence(OrderItem domain) {
        if (domain == null) {
            return null;
        }

        OrderItemPersistenceModel model = new OrderItemPersistenceModel();
        model.setId(domain.getId());
        model.setOrderId(domain.getOrderId());
        model.setMenuItemId(domain.getMenuItemId());
        model.setMenuItemNameSnapshot(domain.getMenuItemNameSnapshot());
        model.setUnitPriceSnapshot(domain.getUnitPriceSnapshot());
        model.setQuantity(domain.getQuantity());
        model.setLineTotal(domain.getLineTotal());
        return model;
    }
}
