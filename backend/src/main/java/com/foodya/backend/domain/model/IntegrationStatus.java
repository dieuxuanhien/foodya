package com.foodya.backend.domain.model;

public record IntegrationStatus(
        String key,
        boolean configured,
        String source
) {
}
