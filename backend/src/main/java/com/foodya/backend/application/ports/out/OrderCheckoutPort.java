package com.foodya.backend.application.ports.out;

import com.foodya.backend.application.dto.MenuItemModel;
import com.foodya.backend.application.dto.OrderItemModel;
import com.foodya.backend.application.dto.OrderModel;
import com.foodya.backend.application.dto.OrderPaymentModel;
import com.foodya.backend.application.dto.RestaurantModel;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderCheckoutPort {

    Optional<OrderModel> findByCustomerUserIdAndIdempotencyKey(UUID customerUserId, String idempotencyKey);

    Optional<RestaurantModel> findActiveRestaurantById(UUID restaurantId);

    Optional<MenuItemModel> findMenuItemById(UUID menuItemId);

    OrderModel saveOrder(OrderModel order);

    void saveOrderItems(List<OrderItemModel> items);

    void saveOrderPayment(OrderPaymentModel payment);
}