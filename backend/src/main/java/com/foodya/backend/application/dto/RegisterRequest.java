package com.foodya.backend.application.dto;

import com.foodya.backend.domain.value_objects.UserRole;

public record RegisterRequest(
        String username,
        String email,
        String phoneNumber,
        String fullName,
        String password,
        UserRole role
) {
}
