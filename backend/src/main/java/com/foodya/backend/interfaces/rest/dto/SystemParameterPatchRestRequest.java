package com.foodya.backend.interfaces.rest.dto;

import com.foodya.backend.domain.model.ParameterValueType;

public record SystemParameterPatchRestRequest(
        ParameterValueType valueType,
        String value,
        Boolean runtimeApplicable,
        String description
) {
}
