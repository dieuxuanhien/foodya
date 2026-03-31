package com.foodya.backend.application.ports.out;

import com.foodya.backend.application.event.OrderNotificationEvent;

public interface OrderEventPublisherPort {

    void publishOrderNotification(OrderNotificationEvent event);
}
