package com.foodya.backend.interfaces.rest.dto;

import com.foodya.backend.domain.value_objects.ParameterValueType;
import com.foodya.backend.interfaces.rest.validation.AtLeastOneField;
import jakarta.validation.constraints.Size;

@AtLeastOneField(fields = {"valueType", "value", "runtimeApplicable", "description"}, message = "at least one field must be provided")
public record SystemParameterPatchApiRequest(
        ParameterValueType valueType,
        @Size(max = 2048, message = "must be <= 2048 characters")
        String value,
        Boolean runtimeApplicable,
        @Size(max = 1024, message = "must be <= 1024 characters")
        String description
) {
}
