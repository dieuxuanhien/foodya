package com.foodya.backend.application.port.out;

import com.foodya.backend.domain.persistence.OrderPayment;

public interface OrderPaymentPort {

    OrderPayment save(OrderPayment payment);
}
