package com.foodya.backend.application.dto;

public record UpdateCartItemRequest(
        int quantity,
        String note
) {
}
