package com.foodya.backend.application.ports.in;

import com.foodya.backend.application.dto.CreateOrderFromCartRequest;
import com.foodya.backend.application.dto.OrderCostReviewView;
import com.foodya.backend.application.dto.OrderCreatedView;

import java.util.UUID;

public interface OrderCheckoutUseCase {

    OrderCostReviewView reviewCurrentCartCost(UUID customerUserId, CreateOrderFromCartRequest request);

    OrderCreatedView createOrder(UUID customerUserId, String idempotencyKey, CreateOrderFromCartRequest request);
}
