package com.foodya.backend.infrastructure.repository;

import com.foodya.backend.infrastructure.persistence.models.OrderPersistenceModel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Raw JPA repository for OrderPersistenceModel.
 * This is INTERNAL to infrastructure layer — always use adapter.
 */
public interface OrderRepository extends JpaRepository<OrderPersistenceModel, UUID> {

    Optional<OrderPersistenceModel> findByCustomerUserIdAndIdempotencyKey(UUID customerUserId, String idempotencyKey);
}
