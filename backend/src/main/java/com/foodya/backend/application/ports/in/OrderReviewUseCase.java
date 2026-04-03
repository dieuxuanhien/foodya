package com.foodya.backend.application.ports.in;

import com.foodya.backend.application.dto.OrderReviewView;

import java.util.List;
import java.util.UUID;

public interface OrderReviewUseCase {

    OrderReviewView createReview(UUID customerUserId, UUID orderId, int stars, String comment);

    OrderReviewView merchantRespond(UUID merchantUserId, UUID reviewId, String response);

    List<OrderReviewView> listRestaurantReviews(UUID restaurantId);
}
