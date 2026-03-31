package com.foodya.backend.application.port.out;

import com.foodya.backend.domain.persistence.OrderItem;

import java.util.List;

public interface OrderItemPort {

    List<OrderItem> saveAll(List<OrderItem> items);
}
