package com.foodya.backend.application.dto;

import com.foodya.backend.domain.model.ParameterValueType;

public record SystemParameterPatchRequest(
        ParameterValueType valueType,
        String value,
        Boolean runtimeApplicable,
        String description
) {
}
