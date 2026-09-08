package com.foodya.backend.application.ports.out;

import com.foodya.backend.application.dto.PaginatedResult;
import com.foodya.backend.domain.value_objects.OrderStatus;
import com.foodya.backend.domain.entities.Order;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderManagementPort {

    Optional<Order> findById(UUID orderId);

    PaginatedResult<Order> findByCustomerUserIdOrderByPlacedAtDesc(UUID customerUserId, int page, int size);

    PaginatedResult<Order> findByRestaurantIdOrderByPlacedAtDesc(UUID restaurantId, int page, int size);

    List<Order> findByStatusInOrderByPlacedAtAsc(Collection<OrderStatus> statuses);

    Order save(Order order);
}
