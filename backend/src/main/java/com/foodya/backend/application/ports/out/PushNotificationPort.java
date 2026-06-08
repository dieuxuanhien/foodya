package com.foodya.backend.application.ports.out;

import java.util.UUID;

public interface PushNotificationPort {

    DeliveryResult sendToUser(UUID receiverUserId, String title, String message, UUID orderId, String eventType);

    record DeliveryResult(boolean delivered, String providerResponse) {
    }
}
