package com.foodya.backend.infrastructure.repository;

import com.foodya.backend.domain.value_objects.OrderStatus;
import com.foodya.backend.domain.entities.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AdminOrderRepository extends JpaRepository<Order, UUID> {

    Page<Order> findByStatus(OrderStatus status, Pageable pageable);
}
