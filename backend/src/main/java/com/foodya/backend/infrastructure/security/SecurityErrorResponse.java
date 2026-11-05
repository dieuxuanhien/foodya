package com.foodya.backend.infrastructure.security;

public record SecurityErrorResponse(String code, String message, Object detail, String traceId) {}