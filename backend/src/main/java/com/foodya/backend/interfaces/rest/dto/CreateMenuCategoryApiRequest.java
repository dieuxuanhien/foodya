package com.foodya.backend.interfaces.rest.dto;

import com.foodya.backend.application.dto.CreateMenuCategoryRequest;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateMenuCategoryApiRequest(
        @NotBlank @Size(max = 255) String name,
        @NotNull @Min(0) Integer sortOrder,
        @NotNull Boolean isActive
) {
    public CreateMenuCategoryRequest toApplicationDto() {
        return new CreateMenuCategoryRequest(
                name, sortOrder, isActive
        );
    }
}
