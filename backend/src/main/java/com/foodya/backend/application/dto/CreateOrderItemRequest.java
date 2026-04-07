package com.foodya.backend.application.dto;

public record CreateOrderItemRequest(
	String menuItemId,
	int quantity
) {
}
