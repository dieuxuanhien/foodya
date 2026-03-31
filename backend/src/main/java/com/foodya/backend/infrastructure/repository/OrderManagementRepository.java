package com.foodya.backend.infrastructure.repository;

import com.foodya.backend.domain.model.OrderStatus;
import com.foodya.backend.domain.persistence.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface OrderManagementRepository extends JpaRepository<Order, UUID> {

    List<Order> findByCustomerUserIdOrderByPlacedAtDesc(UUID customerUserId);

    List<Order> findByRestaurantIdOrderByPlacedAtDesc(UUID restaurantId);

    List<Order> findByStatusInOrderByPlacedAtAsc(Collection<OrderStatus> statuses);
}
