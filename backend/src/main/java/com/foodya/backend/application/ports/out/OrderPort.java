package com.foodya.backend.application.ports.out;

import com.foodya.backend.domain.entities.Order;

import java.util.Optional;
import java.util.UUID;

public interface OrderPort {

    Optional<Order> findByCustomerUserIdAndIdempotencyKey(UUID customerUserId, String idempotencyKey);

    Order save(Order order);
}
