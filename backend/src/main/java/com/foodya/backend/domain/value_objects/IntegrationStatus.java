package com.foodya.backend.domain.value_objects;

public record IntegrationStatus(
        String key,
        boolean configured,
        String source
) {
}
