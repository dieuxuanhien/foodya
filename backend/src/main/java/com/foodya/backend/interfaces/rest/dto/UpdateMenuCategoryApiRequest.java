package com.foodya.backend.interfaces.rest.dto;

import com.foodya.backend.application.dto.UpdateMenuCategoryRequest;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateMenuCategoryApiRequest(
        @Size(max = 255) String name,
        @Min(0) Integer sortOrder,
        Boolean isActive
) {
    public UpdateMenuCategoryRequest toApplicationDto() {
        return new UpdateMenuCategoryRequest(
                name, sortOrder, isActive
        );
    }
}
