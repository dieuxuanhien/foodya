package com.foodya.backend.application.port.out;

import com.foodya.backend.domain.persistence.Order;

import java.util.Optional;
import java.util.UUID;

public interface OrderPort {

    Optional<Order> findByCustomerUserIdAndIdempotencyKey(UUID customerUserId, String idempotencyKey);

    Order save(Order order);
}
