package com.foodya.backend.domain.value_objects;

public enum PaymentMethod {
    COD,
    VNPAY,

    /**
     * VNPAY flow entry points. All are persisted as {@code VNPAY}; the specific channel is what the
     * user selects in the gateway UI ("Thẻ ATM", "QR Pay", ...), which VNPAY exposes on its own page.
     */
    VNPAY_QR,
    VNPAY_ATM,
    VNPAY_CARD;

    public boolean isOnlineGateway() {
        return this != COD;
    }

    /** Normalises channel-specific methods down to the persisted gateway method. */
    public PaymentMethod toPersistedMethod() {
        return this == COD ? COD : VNPAY;
    }
}
