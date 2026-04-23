package com.foodya.backend.infrastructure.repository;

import com.foodya.backend.infrastructure.persistence.models.OrderItemPersistenceModel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface OrderItemRepository extends JpaRepository<OrderItemPersistenceModel, UUID> {
}
