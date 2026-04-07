package com.foodya.backend.application.ports.out;

import java.util.UUID;

public interface RestaurantImageStoragePort {

    String storeBackground(UUID restaurantId, String originalFileName, String contentType, byte[] content);

    String storeAvatar(UUID restaurantId, String originalFileName, String contentType, byte[] content);
}
