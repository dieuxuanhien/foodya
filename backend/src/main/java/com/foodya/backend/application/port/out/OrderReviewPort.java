package com.foodya.backend.application.port.out;

import com.foodya.backend.domain.persistence.OrderReview;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderReviewPort {

    OrderReview save(OrderReview review);

    Optional<OrderReview> findById(UUID reviewId);

    Optional<OrderReview> findByOrderId(UUID orderId);

    List<OrderReview> findByRestaurantIdOrderByCreatedAtDesc(UUID restaurantId);
}
