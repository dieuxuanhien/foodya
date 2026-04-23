package com.foodya.backend.application.dto;

public record TokenPairResult(
        String accessToken,
        String refreshToken
) {
}
