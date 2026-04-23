package com.foodya.backend.application.ports.out;

import java.util.UUID;

public interface MenuItemImageStoragePort {

    String store(UUID menuItemId, String originalFileName, String contentType, byte[] content);
}
