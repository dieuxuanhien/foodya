package com.foodya.backend.interfaces.rest.dto;

public record ApiErrorResponse(
        String code,
        String message,
        Object details,
        String traceId
) {
}
