package com.foodya.backend.interfaces.rest.dto;

import com.foodya.backend.domain.model.ParameterValueType;

import java.time.OffsetDateTime;

public record SystemParameterResponse(
        String key,
        ParameterValueType valueType,
        String value,
        boolean runtimeApplicable,
        int version,
        String description,
        String updatedBy,
        OffsetDateTime updatedAt
) {
}
