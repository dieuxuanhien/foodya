package com.foodya.backend.interfaces.rest.dto;

import com.foodya.backend.domain.value_objects.ParameterValueType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SystemParameterPutApiRequest(
        @NotNull ParameterValueType valueType,
        @NotBlank String value,
        @NotNull Boolean runtimeApplicable,
        String description
) {
}
