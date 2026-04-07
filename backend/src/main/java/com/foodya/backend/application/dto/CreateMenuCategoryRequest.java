package com.foodya.backend.application.dto;

public record CreateMenuCategoryRequest(
	String name,
	Integer sortOrder,
	Boolean isActive
) {
}
