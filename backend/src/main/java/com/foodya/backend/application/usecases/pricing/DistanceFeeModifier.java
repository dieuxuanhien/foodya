package com.foodya.backend.application.usecases.pricing;

import com.foodya.backend.application.ports.in.DeliveryFeeModifier;
import com.foodya.backend.domain.policies.DeliveryFeePolicy;
import com.foodya.backend.domain.value_objects.DeliveryFeeBreakdown;
import com.foodya.backend.domain.value_objects.DeliveryFeeContext;
import com.foodya.backend.domain.value_objects.FeeComponent;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Base fee + over-distance charge. Replaces the inline calculation that used to live in
 * {@code OrderCheckoutService}; the arithmetic itself stays in {@link DeliveryFeePolicy}.
 */
public class DistanceFeeModifier implements DeliveryFeeModifier {

    private final PricingSettings settings;

    public DistanceFeeModifier(PricingSettings settings) {
        this.settings = settings;
    }

    @Override
    public DeliveryFeeBreakdown apply(DeliveryFeeBreakdown current, DeliveryFeeContext context) {
        BigDecimal distance = context.routeDistanceKm();
        BigDecimal baseFee = settings.baseFee();
        BigDecimal baseDistance = settings.baseDistanceKm();
        BigDecimal perKm = settings.feePerKm();

        BigDecimal distanceCharge = distance.compareTo(baseDistance) <= 0
                ? BigDecimal.ZERO
                : distance.subtract(baseDistance).multiply(perKm).setScale(0, java.math.RoundingMode.HALF_UP);

        List<FeeComponent> components = new ArrayList<>(current.components());
        components.add(new FeeComponent("BASE", "Base delivery",
                baseFee.setScale(0, java.math.RoundingMode.HALF_UP)));
        if (distanceCharge.signum() > 0) {
            components.add(new FeeComponent("DISTANCE",
                    "Distance (" + context.routeDistanceKm().stripTrailingZeros().toPlainString() + " km)",
                    distanceCharge));
        }
        return new DeliveryFeeBreakdown(baseFee.add(distanceCharge), components);
    }

    @Override
    public String name() {
        return "DISTANCE";
    }

    @Override
    public int order() {
        return 1;
    }
}
