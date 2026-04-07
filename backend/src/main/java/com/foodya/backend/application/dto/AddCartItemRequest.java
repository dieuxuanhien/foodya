package com.foodya.backend.application.dto;

public record AddCartItemRequest(
        String menuItemId,
        int quantity,
        String note
) {
}
