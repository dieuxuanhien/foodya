package com.foodya.backend.application.dto;

import com.foodya.backend.domain.model.UserRole;
import com.foodya.backend.domain.model.UserStatus;

import java.util.UUID;

public record AdminUserSummaryView(
        UUID id,
        String username,
        String email,
        String phoneNumber,
        String fullName,
        UserRole role,
        UserStatus status
) {
}
