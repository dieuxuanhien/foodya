package com.foodya.backend.infrastructure.repository;

import com.foodya.backend.domain.persistence.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    Optional<Order> findByCustomerUserIdAndIdempotencyKey(UUID customerUserId, String idempotencyKey);
}
