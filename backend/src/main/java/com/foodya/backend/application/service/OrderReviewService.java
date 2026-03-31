package com.foodya.backend.application.service;

import com.foodya.backend.application.dto.OrderReviewView;
import com.foodya.backend.application.exception.ForbiddenException;
import com.foodya.backend.application.exception.NotFoundException;
import com.foodya.backend.application.exception.ValidationException;
import com.foodya.backend.application.port.out.OrderManagementPort;
import com.foodya.backend.application.port.out.OrderReviewPort;
import com.foodya.backend.application.port.out.RestaurantPort;
import com.foodya.backend.domain.model.OrderStatus;
import com.foodya.backend.domain.persistence.Order;
import com.foodya.backend.domain.persistence.OrderReview;
import com.foodya.backend.domain.persistence.Restaurant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class OrderReviewService {

    private final OrderManagementPort orderManagementPort;
    private final OrderReviewPort orderReviewPort;
    private final RestaurantPort restaurantPort;

    public OrderReviewService(OrderManagementPort orderManagementPort,
                              OrderReviewPort orderReviewPort,
                              RestaurantPort restaurantPort) {
        this.orderManagementPort = orderManagementPort;
        this.orderReviewPort = orderReviewPort;
        this.restaurantPort = restaurantPort;
    }

    @Transactional
    public OrderReviewView createReview(UUID customerUserId, UUID orderId, int stars, String comment) {
        Order order = requireOrder(orderId);
        if (!order.getCustomerUserId().equals(customerUserId)) {
            throw new ForbiddenException("order does not belong to customer");
        }
        if (order.getStatus() != OrderStatus.SUCCESS) {
            throw new ValidationException("review is allowed only for SUCCESS orders", Map.of("status", "must be SUCCESS"));
        }
        if (orderReviewPort.findByOrderId(orderId).isPresent()) {
            throw new ValidationException("order review already exists", Map.of("orderId", "review already submitted"));
        }

        OrderReview review = new OrderReview();
        review.setOrderId(order.getId());
        review.setRestaurantId(order.getRestaurantId());
        review.setCustomerUserId(customerUserId);
        review.setStars(stars);
        review.setComment(normalizeText(comment));

        return toView(orderReviewPort.save(review));
    }

    @Transactional
    public OrderReviewView merchantRespond(UUID merchantUserId, UUID reviewId, String response) {
        OrderReview review = orderReviewPort.findById(reviewId)
                .orElseThrow(() -> new NotFoundException("review not found"));

        Restaurant restaurant = restaurantPort.findById(review.getRestaurantId())
                .orElseThrow(() -> new NotFoundException("restaurant not found"));

        if (!restaurant.getOwnerUserId().equals(merchantUserId)) {
            throw new ForbiddenException("review does not belong to merchant");
        }

        review.setMerchantResponse(normalizeRequiredText(response));
        review.setRespondedAt(OffsetDateTime.now());
        return toView(orderReviewPort.save(review));
    }

    @Transactional(readOnly = true)
    public List<OrderReviewView> listRestaurantReviews(UUID restaurantId) {
        if (restaurantPort.findById(restaurantId).isEmpty()) {
            throw new NotFoundException("restaurant not found");
        }

        return orderReviewPort.findByRestaurantIdOrderByCreatedAtDesc(restaurantId)
                .stream()
                .map(this::toView)
                .toList();
    }

    private Order requireOrder(UUID orderId) {
        return orderManagementPort.findById(orderId)
                .orElseThrow(() -> new NotFoundException("order not found"));
    }

    private OrderReviewView toView(OrderReview review) {
        return new OrderReviewView(
                review.getId(),
                review.getOrderId(),
                review.getRestaurantId(),
                review.getCustomerUserId(),
                review.getStars(),
                review.getComment(),
                review.getMerchantResponse(),
                review.getRespondedAt(),
                review.getCreatedAt()
        );
    }

    private static String normalizeText(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String normalizeRequiredText(String raw) {
        String trimmed = raw == null ? "" : raw.trim();
        if (trimmed.isEmpty()) {
            throw new ValidationException("merchant response is required", Map.of("response", "must not be blank"));
        }
        return trimmed;
    }
}
