package com.foodya.backend.application.ports.out;

import java.util.UUID;

public interface UserAvatarStoragePort {

    String store(UUID userId, String originalFileName, String contentType, byte[] content);
}
