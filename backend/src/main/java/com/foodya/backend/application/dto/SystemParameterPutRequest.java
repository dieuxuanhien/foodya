package com.foodya.backend.application.dto;

import com.foodya.backend.domain.value_objects.ParameterValueType;

public record SystemParameterPutRequest(
        ParameterValueType valueType,
        String value,
        Boolean runtimeApplicable,
        String description
) {
}
