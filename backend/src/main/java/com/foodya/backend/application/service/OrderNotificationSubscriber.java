package com.foodya.backend.application.service;

import com.foodya.backend.application.event.OrderNotificationEvent;
import com.foodya.backend.domain.model.UserRole;
import org.springframework.stereotype.Component;
import org.springframework.context.event.EventListener;

@Component
public class OrderNotificationSubscriber {

    private final NotificationService notificationService;

    public OrderNotificationSubscriber(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @EventListener
    public void onOrderEvent(OrderNotificationEvent event) {
        notificationService.notifyUser(
                event.customerUserId(),
                UserRole.CUSTOMER,
                event.eventType(),
                event.title(),
                event.message(),
                event.orderId()
        );

        notificationService.notifyUser(
                event.merchantUserId(),
                UserRole.MERCHANT,
                event.eventType(),
                event.title(),
                event.message(),
                event.orderId()
        );
    }
}
