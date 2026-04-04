package com.foodya.backend.infrastructure.security;

public record SecurityApiErrorResponse(
        String code,
        String message,
        Object details,
        String traceId
) {
}
