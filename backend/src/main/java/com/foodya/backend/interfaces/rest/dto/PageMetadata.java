package com.foodya.backend.interfaces.rest.dto;

public record PageMetadata(
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
