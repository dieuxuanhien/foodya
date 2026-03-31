package com.foodya.backend.interfaces.rest.dto;

import com.foodya.backend.domain.value_objects.UserRole;
import com.foodya.backend.domain.value_objects.UserStatus;

public record MeResponse(
        String id,
        String username,
        String email,
        String phoneNumber,
        String fullName,
        String avatarUrl,
        UserRole role,
        UserStatus status
) {
}
