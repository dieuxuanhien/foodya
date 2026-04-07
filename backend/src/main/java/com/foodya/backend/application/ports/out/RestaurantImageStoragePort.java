package com.foodya.backend.application.ports.out;

import java.util.UUID;

public interface RestaurantImageStoragePort {

    String store(UUID restaurantId, String originalFileName, String contentType, byte[] content);
}
