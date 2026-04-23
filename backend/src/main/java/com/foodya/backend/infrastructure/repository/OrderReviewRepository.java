package com.foodya.backend.infrastructure.repository;

import com.foodya.backend.infrastructure.persistence.models.OrderReviewPersistenceModel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderReviewRepository extends JpaRepository<OrderReviewPersistenceModel, UUID> {

    Optional<OrderReviewPersistenceModel> findByOrderId(UUID orderId);

    List<OrderReviewPersistenceModel> findByRestaurantIdOrderByCreatedAtDesc(UUID restaurantId);
}
