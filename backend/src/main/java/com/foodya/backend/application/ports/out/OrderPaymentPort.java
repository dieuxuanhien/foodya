package com.foodya.backend.application.ports.out;

import com.foodya.backend.domain.entities.OrderPayment;

import java.util.Optional;
import java.util.UUID;

public interface OrderPaymentPort {

    Optional<OrderPayment> findByOrderId(UUID orderId);

    OrderPayment save(OrderPayment payment);
}
