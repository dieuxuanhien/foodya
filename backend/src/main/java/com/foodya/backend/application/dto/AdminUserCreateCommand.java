package com.foodya.backend.application.dto;

import com.foodya.backend.domain.value_objects.UserRole;
import com.foodya.backend.domain.value_objects.UserStatus;

public record AdminUserCreateCommand(
        String username,
        String email,
        String phoneNumber,
        String fullName,
        String password,
        UserRole role,
        UserStatus status
) {
}
