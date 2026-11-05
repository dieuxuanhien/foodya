package com.foodya.backend.application.usecases.pricing;

import java.math.BigDecimal;

/** Resolved pricing parameters, loaded once per calculation from system_parameters. */
public record PricingSettings(
        BigDecimal baseFee,
        BigDecimal baseDistanceKm,
        BigDecimal feePerKm,
        BigDecimal maxDeliveryFee,
        BigDecimal rainMultiplier,
        BigDecimal heavyRainMultiplier,
        BigDecimal stormMultiplier,
        boolean surgeEnabled,
        BigDecimal surgeThresholdRatio,
        BigDecimal surgeMultiplier,
        BigDecimal surgeMaxMultiplier
) {
    public static PricingSettings defaults() {
        return new PricingSettings(
                BigDecimal.valueOf(10000), BigDecimal.valueOf(2), BigDecimal.valueOf(5000),
                BigDecimal.valueOf(100000),
                new BigDecimal("1.15"), new BigDecimal("1.30"), new BigDecimal("1.50"),
                false, new BigDecimal("2.0"), new BigDecimal("1.20"), new BigDecimal("1.50"));
    }
}
