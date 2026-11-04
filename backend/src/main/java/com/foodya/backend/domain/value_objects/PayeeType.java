package com.foodya.backend.domain.value_objects;

/** Who a settlement is for. Customers are not settled: their refunds are payment-layer records. */
public enum PayeeType {
    MERCHANT,
    DRIVER
}