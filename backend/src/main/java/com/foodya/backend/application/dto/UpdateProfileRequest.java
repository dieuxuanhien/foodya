package com.foodya.backend.application.dto;

public record UpdateProfileRequest(
        String fullName,
        String email,
        String phoneNumber,
        String avatarUrl
) {
}
