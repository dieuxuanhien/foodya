package com.foodya.backend.application.usecases.pricing;

import com.foodya.backend.application.ports.in.DeliveryFeeModifier;
import com.foodya.backend.domain.value_objects.DeliveryFeeBreakdown;
import com.foodya.backend.domain.value_objects.DeliveryFeeContext;
import com.foodya.backend.domain.value_objects.FeeComponent;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Demand/supply surge. Disabled by default ({@code pricing.surge.enabled=false}) because it depends on a
 * demand signal the platform does not yet trust at checkout time; the multiplier is additionally capped.
 */
public class DemandSurgeFeeModifier implements DeliveryFeeModifier {

    private final PricingSettings settings;

    public DemandSurgeFeeModifier(PricingSettings settings) {
        this.settings = settings;
    }

    @Override
    public DeliveryFeeBreakdown apply(DeliveryFeeBreakdown current, DeliveryFeeContext context) {
        if (!settings.surgeEnabled()) {
            return current;
        }
        double ratio = context.demandSupplyRatio();
        if (ratio <= 0 || BigDecimal.valueOf(ratio).compareTo(settings.surgeThresholdRatio()) < 0) {
            return current;
        }
        BigDecimal multiplier = settings.surgeMultiplier()
                .min(settings.surgeMaxMultiplier());
        BigDecimal surcharge = current.totalFee()
                .multiply(multiplier.subtract(BigDecimal.ONE)).setScale(0, RoundingMode.HALF_UP);
        if (surcharge.signum() <= 0) {
            return current;
        }
        String pct = multiplier.subtract(BigDecimal.ONE).multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP).toPlainString();
        List<FeeComponent> components = new ArrayList<>(current.components());
        components.add(new FeeComponent("SURGE", "High demand surcharge (+" + pct + "%)", surcharge));
        return new DeliveryFeeBreakdown(current.totalFee().add(surcharge), components);
    }

    @Override
    public String name() {
        return "SURGE";
    }

    @Override
    public int order() {
        return 3;
    }
}
