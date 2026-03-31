package com.foodya.backend.infrastructure.adapter.event;

import com.foodya.backend.application.event.OrderNotificationEvent;
import com.foodya.backend.application.ports.out.OrderEventPublisherPort;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class SpringOrderEventPublisherAdapter implements OrderEventPublisherPort {

    private final ApplicationEventPublisher applicationEventPublisher;

    public SpringOrderEventPublisherAdapter(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Override
    public void publishOrderNotification(OrderNotificationEvent event) {
        applicationEventPublisher.publishEvent(Objects.requireNonNull(event));
    }
}
