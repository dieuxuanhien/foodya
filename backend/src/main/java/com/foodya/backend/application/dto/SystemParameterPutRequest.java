package com.foodya.backend.application.dto;

import com.foodya.backend.domain.model.ParameterValueType;

public record SystemParameterPutRequest(
        ParameterValueType valueType,
        String value,
        Boolean runtimeApplicable,
        String description
) {
}
