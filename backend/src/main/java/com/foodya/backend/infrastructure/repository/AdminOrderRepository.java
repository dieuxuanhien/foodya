package com.foodya.backend.infrastructure.repository;

import com.foodya.backend.domain.value_objects.OrderStatus;
import com.foodya.backend.infrastructure.persistence.models.OrderPersistenceModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Raw JPA repository for OrderPersistenceModel.
 * This is INTERNAL to infrastructure layer — always use adapter.
 */
public interface AdminOrderRepository extends JpaRepository<OrderPersistenceModel, UUID> {

    Page<OrderPersistenceModel> findByStatus(OrderStatus status, Pageable pageable);
}
