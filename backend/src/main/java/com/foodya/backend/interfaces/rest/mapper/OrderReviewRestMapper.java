package com.foodya.backend.interfaces.rest.mapper;

import com.foodya.backend.application.dto.OrderReviewView;
import com.foodya.backend.interfaces.rest.dto.OrderReviewResponse;

public final class OrderReviewRestMapper {

    private OrderReviewRestMapper() {
    }

    public static OrderReviewResponse toResponse(OrderReviewView view) {
        return new OrderReviewResponse(
                view.reviewId(),
                view.orderId(),
                view.restaurantId(),
                view.customerUserId(),
                view.stars(),
                view.comment(),
                view.merchantResponse(),
                view.respondedAt(),
                view.createdAt()
        );
    }
}
