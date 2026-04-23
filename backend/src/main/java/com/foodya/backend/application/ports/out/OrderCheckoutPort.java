package com.foodya.backend.application.ports.out;

import com.foodya.backend.application.dto.MenuItemData;
import com.foodya.backend.application.dto.OrderItemData;
import com.foodya.backend.application.dto.OrderData;
import com.foodya.backend.application.dto.OrderPaymentData;
import com.foodya.backend.application.dto.RestaurantData;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderCheckoutPort {

    Optional<OrderData> findByCustomerUserIdAndIdempotencyKey(UUID customerUserId, String idempotencyKey);

    Optional<RestaurantData> findActiveRestaurantById(UUID restaurantId);

    Optional<MenuItemData> findMenuItemById(UUID menuItemId);

    OrderData saveOrder(OrderData order);

    void saveOrderItems(List<OrderItemData> items);

    void saveOrderPayment(OrderPaymentData payment);
}