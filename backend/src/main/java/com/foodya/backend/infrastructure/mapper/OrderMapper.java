package com.foodya.backend.infrastructure.mapper;

import com.foodya.backend.domain.entities.Order;
import com.foodya.backend.infrastructure.persistence.models.OrderPersistenceModel;
import org.springframework.stereotype.Component;

/**
 * MAPPER: Order Domain Entity ↔ OrderPersistenceModel
 * 
 * Bidirectional mapping between domain business logic layer and persistence layer.
 * This mapper ensures proper separation of concerns per Clean Architecture:
 * 
 * - Domain layer (Order.java) stays framework-independent and test-friendly
 * - Persistence layer (OrderPersistenceModel.java) handles JPA/database concerns
 * - Mapping happens ONLY at infrastructure boundaries (adapters)
 * 
 * Usage:
 *   // Load from DB: persistence → domain
 *   OrderPersistenceModel persisted = repository.findById(id);
 *   Order domainOrder = mapper.toDomain(persisted);
 * 
 *   // Save to DB: domain → persistence
 *   Order domainOrder = orderService.process(...);
 *   OrderPersistenceModel persisted = mapper.toPersistence(domainOrder);
 *   repository.save(persisted);
 */
@Component
public class OrderMapper {

    /**
     * Convert persistence model to domain entity.
     * Used when loading from database.
     */
    public Order toDomain(OrderPersistenceModel model) {
        if (model == null) {
            return null;
        }

        Order domain = new Order();

        // All fields map 1:1 (names match between domain and persistence model)
        domain.setId(model.getId());
        domain.setOrderCode(model.getOrderCode());
        domain.setCustomerUserId(model.getCustomerUserId());
        domain.setIdempotencyKey(model.getIdempotencyKey());
        domain.setRestaurantId(model.getRestaurantId());
        domain.setStatus(model.getStatus());
        domain.setDeliveryAddress(model.getDeliveryAddress());
        domain.setDeliveryLatitude(model.getDeliveryLatitude());
        domain.setDeliveryLongitude(model.getDeliveryLongitude());
        domain.setCustomerNote(model.getCustomerNote());
        domain.setSubtotalAmount(model.getSubtotalAmount());
        domain.setDeliveryFee(model.getDeliveryFee());
        domain.setTotalAmount(model.getTotalAmount());
        domain.setPaymentMethod(model.getPaymentMethod());
        domain.setPaymentStatus(model.getPaymentStatus());
        domain.setCommissionAmount(model.getCommissionAmount());
        domain.setShippingFeeMarginAmount(model.getShippingFeeMarginAmount());
        domain.setPlatformProfitAmount(model.getPlatformProfitAmount());
        domain.setCancelReason(model.getCancelReason());
        domain.setPlacedAt(model.getPlacedAt());
        domain.setCompletedAt(model.getCompletedAt());

        return domain;
    }

    /**
     * Convert domain entity to persistence model.
     * Used when saving to database.
     */
    public OrderPersistenceModel toPersistence(Order domain) {
        if (domain == null) {
            return null;
        }

        OrderPersistenceModel model = new OrderPersistenceModel();

        // All fields map 1:1 (names match between domain and persistence model)
        model.setId(domain.getId());
        model.setOrderCode(domain.getOrderCode());
        model.setCustomerUserId(domain.getCustomerUserId());
        model.setIdempotencyKey(domain.getIdempotencyKey());
        model.setRestaurantId(domain.getRestaurantId());
        model.setStatus(domain.getStatus());
        model.setDeliveryAddress(domain.getDeliveryAddress());
        model.setDeliveryLatitude(domain.getDeliveryLatitude());
        model.setDeliveryLongitude(domain.getDeliveryLongitude());
        model.setCustomerNote(domain.getCustomerNote());
        model.setSubtotalAmount(domain.getSubtotalAmount());
        model.setDeliveryFee(domain.getDeliveryFee());
        model.setTotalAmount(domain.getTotalAmount());
        model.setPaymentMethod(domain.getPaymentMethod());
        model.setPaymentStatus(domain.getPaymentStatus());
        model.setCommissionAmount(domain.getCommissionAmount());
        model.setShippingFeeMarginAmount(domain.getShippingFeeMarginAmount());
        model.setPlatformProfitAmount(domain.getPlatformProfitAmount());
        model.setCancelReason(domain.getCancelReason());
        model.setPlacedAt(domain.getPlacedAt());
        model.setCompletedAt(domain.getCompletedAt());

        return model;
    }
}
