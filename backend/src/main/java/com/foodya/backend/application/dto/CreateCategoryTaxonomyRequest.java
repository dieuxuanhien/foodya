package com.foodya.backend.application.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateCategoryTaxonomyRequest(
        @NotBlank @Size(max = 64) String code,
        @NotBlank @Size(max = 120) String displayName,
        @Size(max = 500) String description,
        @Size(max = 255) String icon,
        @NotNull @Min(0) @Max(10000) Integer sortOrder,
        @NotNull Boolean isActive
) {
}
