package com.foodya.backend.infrastructure.adapter.integration;

import com.foodya.backend.application.ports.out.PushNotificationPort;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class PushNotificationAdapter implements PushNotificationPort {

    @Override
    public DeliveryResult sendToUser(UUID receiverUserId, String title, String message) {
        return new DeliveryResult(false, "device-token-not-configured");
    }
}
