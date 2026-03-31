package com.foodya.backend.application.port.out;

import com.foodya.backend.application.dto.PaginatedResult;
import com.foodya.backend.domain.model.OrderStatus;
import com.foodya.backend.domain.persistence.Order;

import java.util.Optional;
import java.util.UUID;

public interface AdminOrderPort {

    PaginatedResult<Order> search(OrderStatus status, int page, int size);

    Optional<Order> findById(UUID orderId);

    Order save(Order order);

    void delete(Order order);
}
