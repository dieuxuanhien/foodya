package com.foodya.backend.application.dto;

public record TokenPairResponse(
        String accessToken,
        String refreshToken
) {
}
