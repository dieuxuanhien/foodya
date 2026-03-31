package com.foodya.backend.interfaces.rest.dto;

public record ApiSuccessResponse<T>(
        T data,
        Object meta,
        String traceId
) {
    public static <T> ApiSuccessResponse<T> of(T data, String traceId) {
        return new ApiSuccessResponse<>(data, null, traceId);
    }

    public static <T> ApiSuccessResponse<T> of(T data, Object meta, String traceId) {
        return new ApiSuccessResponse<>(data, meta, traceId);
    }
}
