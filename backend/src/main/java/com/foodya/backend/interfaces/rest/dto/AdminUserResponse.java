package com.foodya.backend.interfaces.rest.dto;

import com.foodya.backend.domain.value_objects.UserRole;
import com.foodya.backend.domain.value_objects.UserStatus;

import java.util.UUID;

public record AdminUserResponse(
        UUID id,
        String username,
        String email,
        String phoneNumber,
        String fullName,
        UserRole role,
        UserStatus status
) {
}
