package com.foodya.backend.interfaces.rest.dto;

import com.foodya.backend.application.dto.UpdateCartItemRequest;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record UpdateCartItemApiRequest(
        @Min(1) int quantity,
        @Size(max = 255)
        String note
) {
    public UpdateCartItemRequest toApplicationDto() {
        return new UpdateCartItemRequest(quantity, note);
    }
}
