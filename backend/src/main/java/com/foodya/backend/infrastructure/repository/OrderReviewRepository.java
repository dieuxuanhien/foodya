package com.foodya.backend.infrastructure.repository;

import com.foodya.backend.domain.entities.OrderReview;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderReviewRepository extends JpaRepository<OrderReview, UUID> {

    Optional<OrderReview> findByOrderId(UUID orderId);

    List<OrderReview> findByRestaurantIdOrderByCreatedAtDesc(UUID restaurantId);
}
