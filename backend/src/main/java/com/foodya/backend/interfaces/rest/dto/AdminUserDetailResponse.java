package com.foodya.backend.interfaces.rest.dto;

import com.foodya.backend.domain.value_objects.UserRole;
import com.foodya.backend.domain.value_objects.UserStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AdminUserDetailResponse(
        UUID id,
        String username,
        String email,
        String phoneNumber,
        String fullName,
        String avatarUrl,
        UserRole role,
        UserStatus status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
