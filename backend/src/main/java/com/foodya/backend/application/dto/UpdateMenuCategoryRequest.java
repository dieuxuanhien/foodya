package com.foodya.backend.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateMenuCategoryRequest(
        @NotBlank String name,
        @NotNull Integer sortOrder,
        @NotNull Boolean isActive
) {
}
