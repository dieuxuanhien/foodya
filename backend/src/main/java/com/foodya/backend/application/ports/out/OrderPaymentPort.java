package com.foodya.backend.application.ports.out;

import com.foodya.backend.domain.entities.OrderPayment;

public interface OrderPaymentPort {

    OrderPayment save(OrderPayment payment);
}
