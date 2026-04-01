package com.foodya.backend.infrastructure.mapper;

import com.foodya.backend.domain.entities.OrderPayment;
import com.foodya.backend.infrastructure.persistence.models.OrderPaymentPersistenceModel;
import org.springframework.stereotype.Component;

@Component
public class OrderPaymentMapper {

    public OrderPayment toDomain(OrderPaymentPersistenceModel model) {
        if (model == null) {
            return null;
        }

        OrderPayment domain = new OrderPayment();
        domain.setId(model.getId());
        domain.setOrderId(model.getOrderId());
        domain.setPaymentMethod(model.getPaymentMethod());
        domain.setPaymentStatus(model.getPaymentStatus());
        domain.setAmount(model.getAmount());
        domain.setPaidAt(model.getPaidAt());
        domain.setExternalRef(model.getExternalRef());
        domain.setCreatedAt(model.getCreatedAt());
        return domain;
    }

    public OrderPaymentPersistenceModel toPersistence(OrderPayment domain) {
        if (domain == null) {
            return null;
        }

        OrderPaymentPersistenceModel model = new OrderPaymentPersistenceModel();
        model.setId(domain.getId());
        model.setOrderId(domain.getOrderId());
        model.setPaymentMethod(domain.getPaymentMethod());
        model.setPaymentStatus(domain.getPaymentStatus());
        model.setAmount(domain.getAmount());
        model.setPaidAt(domain.getPaidAt());
        model.setExternalRef(domain.getExternalRef());
        model.setCreatedAt(domain.getCreatedAt());
        return model;
    }
}
