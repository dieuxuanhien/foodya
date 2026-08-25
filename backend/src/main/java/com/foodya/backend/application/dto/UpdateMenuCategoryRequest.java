package com.foodya.backend.application.dto;

public record UpdateMenuCategoryRequest(
	String name,
	Integer sortOrder,
	Boolean isActive
) {
}
