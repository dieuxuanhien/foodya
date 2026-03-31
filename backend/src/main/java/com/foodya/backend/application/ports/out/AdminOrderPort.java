package com.foodya.backend.application.ports.out;

import com.foodya.backend.application.dto.PaginatedResult;
import com.foodya.backend.domain.value_objects.OrderStatus;
import com.foodya.backend.domain.entities.Order;

import java.util.Optional;
import java.util.UUID;

public interface AdminOrderPort {

    PaginatedResult<Order> search(OrderStatus status, int page, int size);

    Optional<Order> findById(UUID orderId);

    Order save(Order order);

    void delete(Order order);
}
