package com.foodya.backend.infrastructure.adapter;

import com.foodya.backend.application.ports.out.OrderReviewPort;
import com.foodya.backend.domain.entities.OrderReview;
import com.foodya.backend.infrastructure.mapper.OrderReviewMapper;
import com.foodya.backend.infrastructure.repository.OrderReviewRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Component
public class OrderReviewAdapter implements OrderReviewPort {

    private final OrderReviewRepository repository;
    private final OrderReviewMapper mapper;

    public OrderReviewAdapter(OrderReviewRepository repository, OrderReviewMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    @SuppressWarnings("null")
    public OrderReview save(OrderReview review) {
        var saved = repository.save(mapper.toPersistence(Objects.requireNonNull(review)));
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<OrderReview> findById(UUID reviewId) {
        return repository.findById(Objects.requireNonNull(reviewId)).map(mapper::toDomain);
    }

    @Override
    public Optional<OrderReview> findByOrderId(UUID orderId) {
        return repository.findByOrderId(orderId).map(mapper::toDomain);
    }

    @Override
    public List<OrderReview> findByRestaurantIdOrderByCreatedAtDesc(UUID restaurantId) {
        return repository.findByRestaurantIdOrderByCreatedAtDesc(restaurantId)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }
}
