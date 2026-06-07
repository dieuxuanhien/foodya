package com.foodya.backend.application.ports.out;

import com.foodya.backend.application.dto.OrderTrackingPointView;

import java.util.UUID;

public interface OrderTrackingUpdatePublisherPort {

    void publishTrackingPoint(UUID customerUserId, UUID orderId, OrderTrackingPointView point);
}
