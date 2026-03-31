package com.foodya.backend.application.port.out;

import java.util.UUID;

public interface PushNotificationPort {

    DeliveryResult sendToUser(UUID receiverUserId, String title, String message);

    record DeliveryResult(boolean delivered, String providerResponse) {
    }
}
