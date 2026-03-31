package com.foodya.backend.infrastructure.adapter.persistence;

import com.foodya.backend.application.port.out.OrderReviewPort;
import com.foodya.backend.domain.persistence.OrderReview;
import com.foodya.backend.infrastructure.repository.OrderReviewRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class OrderReviewPersistenceAdapter implements OrderReviewPort {

    private final OrderReviewRepository repository;

    public OrderReviewPersistenceAdapter(OrderReviewRepository repository) {
        this.repository = repository;
    }

    @Override
    public OrderReview save(OrderReview review) {
        return repository.save(review);
    }

    @Override
    public Optional<OrderReview> findById(UUID reviewId) {
        return repository.findById(reviewId);
    }

    @Override
    public Optional<OrderReview> findByOrderId(UUID orderId) {
        return repository.findByOrderId(orderId);
    }

    @Override
    public List<OrderReview> findByRestaurantIdOrderByCreatedAtDesc(UUID restaurantId) {
        return repository.findByRestaurantIdOrderByCreatedAtDesc(restaurantId);
    }
}
