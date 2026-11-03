package com.foodya.backend.domain.value_objects;

public enum PaymentTransactionStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED,
    REFUNDED,
    CANCELLED
}
