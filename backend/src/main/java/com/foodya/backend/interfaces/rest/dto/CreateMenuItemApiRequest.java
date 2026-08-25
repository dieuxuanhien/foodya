package com.foodya.backend.interfaces.rest.dto;

import com.foodya.backend.application.dto.CreateMenuItemRequest;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record CreateMenuItemApiRequest(
        @NotBlank @Pattern(regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-5][0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$", message = "must be a valid UUID") String categoryId,
        List<@NotBlank @Size(max = 64) String> taxonomyCodes,
        @NotBlank @Size(max = 255) String name,
        @NotBlank @Size(max = 2000) String description,
        @NotNull @DecimalMin(value = "0.01") BigDecimal price,
        @NotNull Boolean isActive,
        @NotNull Boolean isAvailable
) {
    public CreateMenuItemRequest toApplicationDto() {
        List<String> codes = (taxonomyCodes == null || taxonomyCodes.isEmpty()) ? List.of("MAIN_DISHES") : taxonomyCodes;
        return new CreateMenuItemRequest(
                categoryId, codes, name, description, price, isActive, isAvailable
        );
    }
}
