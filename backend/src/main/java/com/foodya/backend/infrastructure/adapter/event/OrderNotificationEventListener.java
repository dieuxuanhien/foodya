package com.foodya.backend.infrastructure.adapter.event;

import com.foodya.backend.application.event.OrderNotificationEvent;
import com.foodya.backend.application.usecases.NotificationService;
import com.foodya.backend.domain.value_objects.UserRole;
import org.springframework.stereotype.Component;
import org.springframework.context.event.EventListener;

@Component
public class OrderNotificationEventListener {

    private final NotificationService notificationService;

    public OrderNotificationEventListener(NotificationService notificationService) {
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
