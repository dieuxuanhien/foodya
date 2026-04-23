package com.foodya.backend.application.ports.out;

import com.foodya.backend.domain.entities.OrderItem;

import java.util.List;

public interface OrderItemPort {

    List<OrderItem> saveAll(List<OrderItem> items);
}
