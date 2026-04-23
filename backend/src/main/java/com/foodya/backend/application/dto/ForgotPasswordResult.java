package com.foodya.backend.application.dto;

public record ForgotPasswordResult(
        String challengeToken,
        String deliveryHint
) {
}
