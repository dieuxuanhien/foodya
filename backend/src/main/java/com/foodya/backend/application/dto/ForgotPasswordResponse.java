package com.foodya.backend.application.dto;

public record ForgotPasswordResponse(
        String challengeToken,
        String deliveryHint
) {
}
