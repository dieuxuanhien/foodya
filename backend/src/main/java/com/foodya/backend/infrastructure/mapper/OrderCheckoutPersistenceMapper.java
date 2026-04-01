package com.foodya.backend.infrastructure.mapper;

import com.foodya.backend.application.dto.OrderItemModel;
import com.foodya.backend.application.dto.OrderModel;
import com.foodya.backend.application.dto.OrderPaymentModel;
import com.foodya.backend.domain.entities.Order;
import com.foodya.backend.domain.entities.OrderItem;
import com.foodya.backend.domain.entities.OrderPayment;

public final class OrderCheckoutPersistenceMapper {

    private OrderCheckoutPersistenceMapper() {
    }

    public static OrderModel toModel(Order entity) {
        OrderModel model = new OrderModel();
        model.setId(entity.getId());
        model.setOrderCode(entity.getOrderCode());
        model.setCustomerUserId(entity.getCustomerUserId());
        model.setIdempotencyKey(entity.getIdempotencyKey());
        model.setRestaurantId(entity.getRestaurantId());
        model.setStatus(entity.getStatus());
        model.setDeliveryAddress(entity.getDeliveryAddress());
        model.setDeliveryLatitude(entity.getDeliveryLatitude());
        model.setDeliveryLongitude(entity.getDeliveryLongitude());
        model.setCustomerNote(entity.getCustomerNote());
        model.setSubtotalAmount(entity.getSubtotalAmount());
        model.setDeliveryFee(entity.getDeliveryFee());
        model.setTotalAmount(entity.getTotalAmount());
        model.setPaymentMethod(entity.getPaymentMethod());
        model.setPaymentStatus(entity.getPaymentStatus());
        model.setCommissionAmount(entity.getCommissionAmount());
        model.setShippingFeeMarginAmount(entity.getShippingFeeMarginAmount());
        model.setPlatformProfitAmount(entity.getPlatformProfitAmount());
        return model;
    }

    public static void copyToEntity(OrderModel model, Order entity) {
        entity.setOrderCode(model.getOrderCode());
        entity.setCustomerUserId(model.getCustomerUserId());
        entity.setIdempotencyKey(model.getIdempotencyKey());
        entity.setRestaurantId(model.getRestaurantId());
        entity.setStatus(model.getStatus());
        entity.setDeliveryAddress(model.getDeliveryAddress());
        entity.setDeliveryLatitude(model.getDeliveryLatitude());
        entity.setDeliveryLongitude(model.getDeliveryLongitude());
        entity.setCustomerNote(model.getCustomerNote());
        entity.setSubtotalAmount(model.getSubtotalAmount());
        entity.setDeliveryFee(model.getDeliveryFee());
        entity.setTotalAmount(model.getTotalAmount());
        entity.setPaymentMethod(model.getPaymentMethod());
        entity.setPaymentStatus(model.getPaymentStatus());
        entity.setCommissionAmount(model.getCommissionAmount());
        entity.setShippingFeeMarginAmount(model.getShippingFeeMarginAmount());
        entity.setPlatformProfitAmount(model.getPlatformProfitAmount());
    }

    public static OrderItem toEntity(OrderItemModel model) {
        OrderItem entity = new OrderItem();
        entity.setOrderId(model.getOrderId());
        entity.setMenuItemId(model.getMenuItemId());
        entity.setMenuItemNameSnapshot(model.getMenuItemNameSnapshot());
        entity.setUnitPriceSnapshot(model.getUnitPriceSnapshot());
        entity.setQuantity(model.getQuantity());
        entity.setLineTotal(model.getLineTotal());
        return entity;
    }

    public static OrderPayment toEntity(OrderPaymentModel model) {
        OrderPayment entity = new OrderPayment();
        entity.setOrderId(model.getOrderId());
        entity.setPaymentMethod(model.getPaymentMethod());
        entity.setPaymentStatus(model.getPaymentStatus());
        entity.setAmount(model.getAmount());
        return entity;
    }
}