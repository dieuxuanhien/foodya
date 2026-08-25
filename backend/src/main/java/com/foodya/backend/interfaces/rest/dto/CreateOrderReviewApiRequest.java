package com.foodya.backend.interfaces.rest.dto;

import com.foodya.backend.application.dto.CreateOrderReviewRequest;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record CreateOrderReviewApiRequest(
        @Min(1) @Max(5) int stars,
        @Size(max = 1000) String comment
) {
    public CreateOrderReviewRequest toApplicationDto() {
        return new CreateOrderReviewRequest(stars, comment);
    }
}
