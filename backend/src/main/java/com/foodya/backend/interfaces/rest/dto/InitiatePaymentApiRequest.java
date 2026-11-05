package com.foodya.backend.interfaces.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record InitiatePaymentApiRequest(
        @NotBlank
        @Pattern(regexp = "COD|VNPAY|VNPAY_QR|VNPAY_ATM|VNPAY_CARD", flags = Pattern.Flag.CASE_INSENSITIVE,
                message = "must be one of COD, VNPAY, VNPAY_QR, VNPAY_ATM, VNPAY_CARD")
        String paymentMethod,
        String returnUrl
) {}
