package com.foodya.backend.application.dto;

import java.time.OffsetDateTime;
import java.util.Map;

public record TokenClaims(
        String subject,
        String id,
        OffsetDateTime expiresAt,
        Map<String, Object> claims
) {

    public String getString(String key) {
        Object value = claims.get(key);
        return value == null ? null : value.toString();
    }
}