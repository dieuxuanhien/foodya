package com.foodya.backend.interfaces.rest.dto;

import com.foodya.backend.application.dto.AddCartItemRequest;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddCartItemApiRequest(
        @NotBlank String menuItemId,
        @Min(1) int quantity,
        @Size(max = 255)
        String note
) {
    public AddCartItemRequest toApplicationDto() {
        return new AddCartItemRequest(menuItemId, quantity, note);
    }
}
