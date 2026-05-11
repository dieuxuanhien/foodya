package com.foodya.backend.application.dto;

import com.foodya.backend.domain.value_objects.UserRole;
import com.foodya.backend.domain.value_objects.UserStatus;

public record AdminUserUpdateCommand(
        String username,
        String email,
        String phoneNumber,
        String fullName,
        UserRole role,
        UserStatus status
) {
}
