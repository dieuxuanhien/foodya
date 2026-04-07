package com.foodya.backend.infrastructure.mapper;

import com.foodya.backend.application.dto.OrderItemData;
import com.foodya.backend.application.dto.OrderData;
import com.foodya.backend.application.dto.OrderPaymentData;
import com.foodya.backend.domain.entities.Order;
import com.foodya.backend.domain.entities.OrderItem;
import com.foodya.backend.domain.entities.OrderPayment;

public final class OrderCheckoutPersistenceMapper {

    private OrderCheckoutPersistenceMapper() {
    }

    public static OrderData toData(Order entity) {
        OrderData data = new OrderData();
        data.setId(entity.getId());
        data.setOrderCode(entity.getOrderCode());
        data.setCustomerUserId(entity.getCustomerUserId());
        data.setIdempotencyKey(entity.getIdempotencyKey());
        data.setRestaurantId(entity.getRestaurantId());
        data.setStatus(entity.getStatus());
        data.setDeliveryAddress(entity.getDeliveryAddress());
        data.setDeliveryLatitude(entity.getDeliveryLatitude());
        data.setDeliveryLongitude(entity.getDeliveryLongitude());
        data.setCustomerNote(entity.getCustomerNote());
        data.setSubtotalAmount(entity.getSubtotalAmount());
        data.setDeliveryFee(entity.getDeliveryFee());
        data.setTotalAmount(entity.getTotalAmount());
        data.setPaymentMethod(entity.getPaymentMethod());
        data.setPaymentStatus(entity.getPaymentStatus());
        data.setCommissionAmount(entity.getCommissionAmount());
        data.setShippingFeeMarginAmount(entity.getShippingFeeMarginAmount());
        data.setPlatformProfitAmount(entity.getPlatformProfitAmount());
        return data;
    }

    public static void copyToEntity(OrderData model, Order entity) {
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

    public static OrderItem toEntity(OrderItemData model) {
        OrderItem entity = new OrderItem();
        entity.setOrderId(model.getOrderId());
        entity.setMenuItemId(model.getMenuItemId());
        entity.setMenuItemNameSnapshot(model.getMenuItemNameSnapshot());
        entity.setUnitPriceSnapshot(model.getUnitPriceSnapshot());
        entity.setQuantity(model.getQuantity());
        entity.setLineTotal(model.getLineTotal());
        return entity;
    }

    public static OrderPayment toEntity(OrderPaymentData model) {
        OrderPayment entity = new OrderPayment();
        entity.setOrderId(model.getOrderId());
        entity.setPaymentMethod(model.getPaymentMethod());
        entity.setPaymentStatus(model.getPaymentStatus());
        entity.setAmount(model.getAmount());
        return entity;
    }
}