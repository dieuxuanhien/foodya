package com.foodya.backend.interfaces.rest.dto;

public record PageMeta(
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
