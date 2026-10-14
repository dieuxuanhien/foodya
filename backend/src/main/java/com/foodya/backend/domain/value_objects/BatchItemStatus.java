package com.foodya.backend.domain.value_objects;

/**
 * Status of an individual order within a batch.
 */
public enum BatchItemStatus {
    PENDING,
    PICKED_UP,
    DELIVERING,
    DELIVERED,
    FAILED
}
