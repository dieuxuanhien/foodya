package com.foodya.backend.domain.value_objects;

/**
 * Status of an order batch through its lifecycle.
 */
public enum BatchStatus {
    /** Batch created by solver, awaiting driver pickup flow */
    PENDING,
    /** Driver has started working on the batch */
    ACTIVE,
    /** All orders in the batch delivered successfully */
    COMPLETED,
    /** Batch cancelled (e.g., driver went offline) */
    CANCELLED,
    /** Some orders completed, some failed */
    PARTIAL
}
