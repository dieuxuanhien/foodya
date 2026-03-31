package com.foodya.backend.application.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record CreateOrderReviewRequest(
        @Min(1) @Max(5) int stars,
        @Size(max = 2000) String comment
) {
}
