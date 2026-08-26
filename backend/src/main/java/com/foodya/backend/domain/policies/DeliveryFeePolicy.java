package com.foodya.backend.domain.policies;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Pure Java domain policy for distance delivery fee calculations,
 * base fees, per-km rates, multipliers, and currency rounding.
 */
public class DeliveryFeePolicy {

    public static final BigDecimal DEFAULT_BASE_FEE = BigDecimal.valueOf(10000);
    public static final BigDecimal DEFAULT_BASE_DISTANCE_KM = BigDecimal.valueOf(2);
    public static final BigDecimal DEFAULT_FEE_PER_KM = BigDecimal.valueOf(5000);
    public static final BigDecimal DEFAULT_MAX_DELIVERY_KM = BigDecimal.valueOf(15);
    public static final BigDecimal VND_ROUNDING_STEP = BigDecimal.valueOf(1000);

    private DeliveryFeePolicy() {
        // Utility class
    }

    /**
     * Calculates the delivery fee based on distance, base fee, base distance limit, and per-km rate.
     */
    public static BigDecimal calculateDeliveryFee(BigDecimal distanceKm,
                                                   BigDecimal baseFee,
                                                   BigDecimal baseDistanceKm,
                                                   BigDecimal feePerKm,
                                                   int minorUnit,
                                                   RoundingMode roundingMode) {
        if (distanceKm == null || distanceKm.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Distance must not be negative");
        }

        BigDecimal effectiveBaseFee = baseFee != null ? baseFee : DEFAULT_BASE_FEE;
        BigDecimal effectiveBaseDist = baseDistanceKm != null ? baseDistanceKm : DEFAULT_BASE_DISTANCE_KM;
        BigDecimal effectiveFeePerKm = feePerKm != null ? feePerKm : DEFAULT_FEE_PER_KM;

        BigDecimal fee;
        if (distanceKm.compareTo(effectiveBaseDist) <= 0) {
            fee = effectiveBaseFee;
        } else {
            BigDecimal extraKm = distanceKm.subtract(effectiveBaseDist);
            fee = effectiveBaseFee.add(extraKm.multiply(effectiveFeePerKm));
        }

        return roundAmount(fee, minorUnit, roundingMode);
    }

    /**
     * Overload with default base fee, distance, and rate.
     */
    public static BigDecimal calculateDeliveryFee(BigDecimal distanceKm, int minorUnit, RoundingMode roundingMode) {
        return calculateDeliveryFee(distanceKm, DEFAULT_BASE_FEE, DEFAULT_BASE_DISTANCE_KM, DEFAULT_FEE_PER_KM, minorUnit, roundingMode);
    }

    /**
     * Rounds amounts to specified minor unit scale and rounding mode.
     */
    public static BigDecimal roundAmount(BigDecimal value, int minorUnit, RoundingMode roundingMode) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        return value.setScale(minorUnit, roundingMode != null ? roundingMode : RoundingMode.HALF_UP);
    }

    /**
     * Rounds amount to the nearest 1,000 VND step (or custom currency step).
     */
    public static BigDecimal roundToCurrencyStep(BigDecimal amount, BigDecimal step) {
        if (amount == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal effectiveStep = (step != null && step.compareTo(BigDecimal.ZERO) > 0) ? step : VND_ROUNDING_STEP;
        return amount.divide(effectiveStep, 0, RoundingMode.HALF_UP).multiply(effectiveStep);
    }

    /**
     * Calculates commission amount from subtotal and percent rate.
     */
    public static BigDecimal calculateCommission(BigDecimal subtotal,
                                                  BigDecimal commissionRatePercent,
                                                  int minorUnit,
                                                  RoundingMode roundingMode) {
        if (subtotal == null || commissionRatePercent == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal rawCommission = subtotal.multiply(commissionRatePercent)
                .divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP);
        return roundAmount(rawCommission, minorUnit, roundingMode);
    }

    /**
     * Calculates shipping fee margin amount from delivery fee and margin percent.
     */
    public static BigDecimal calculateShippingMargin(BigDecimal deliveryFee,
                                                      BigDecimal shippingMarginRatePercent,
                                                      int minorUnit,
                                                      RoundingMode roundingMode) {
        if (deliveryFee == null || shippingMarginRatePercent == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal rawMargin = deliveryFee.multiply(shippingMarginRatePercent)
                .divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP);
        return roundAmount(rawMargin, minorUnit, roundingMode);
    }

    /**
     * Calculates total platform profit by summing commission and shipping margin amounts.
     */
    public static BigDecimal calculatePlatformProfit(BigDecimal commissionAmount,
                                                     BigDecimal shippingFeeMarginAmount,
                                                     int minorUnit,
                                                     RoundingMode roundingMode) {
        BigDecimal total = (commissionAmount != null ? commissionAmount : BigDecimal.ZERO)
                .add(shippingFeeMarginAmount != null ? shippingFeeMarginAmount : BigDecimal.ZERO);
        return roundAmount(total, minorUnit, roundingMode);
    }
}
