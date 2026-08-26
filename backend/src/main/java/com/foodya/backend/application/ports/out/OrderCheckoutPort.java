package com.foodya.backend.application.ports.out;

import com.foodya.backend.domain.entities.MenuItem;
import com.foodya.backend.domain.entities.Order;
import com.foodya.backend.domain.entities.OrderItem;
import com.foodya.backend.domain.entities.OrderPayment;
import com.foodya.backend.domain.entities.Restaurant;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderCheckoutPort {

    Optional<Order> findByCustomerUserIdAndIdempotencyKey(UUID customerUserId, String idempotencyKey);

    Optional<Restaurant> findActiveRestaurantById(UUID restaurantId);

    Optional<MenuItem> findMenuItemById(UUID menuItemId);

    Order saveOrder(Order order);

    void saveOrderItems(List<OrderItem> items);

    void saveOrderPayment(OrderPayment payment);
}