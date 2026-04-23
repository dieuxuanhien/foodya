package com.foodya.backend.application.ports.out;

import com.foodya.backend.domain.value_objects.OrderStatus;
import com.foodya.backend.domain.entities.Order;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderManagementPort {

    Optional<Order> findById(UUID orderId);

    List<Order> findByCustomerUserIdOrderByPlacedAtDesc(UUID customerUserId);

    List<Order> findByRestaurantIdOrderByPlacedAtDesc(UUID restaurantId);

    List<Order> findByStatusInOrderByPlacedAtAsc(Collection<OrderStatus> statuses);

    Order save(Order order);
}
