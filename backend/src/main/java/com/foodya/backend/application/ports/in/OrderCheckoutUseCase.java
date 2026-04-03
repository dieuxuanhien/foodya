package com.foodya.backend.application.ports.in;

import com.foodya.backend.application.dto.CreateOrderRequest;
import com.foodya.backend.application.dto.OrderCreatedView;

import java.util.UUID;

public interface OrderCheckoutUseCase {

    OrderCreatedView createOrder(UUID customerUserId, String idempotencyKey, CreateOrderRequest request);
}
