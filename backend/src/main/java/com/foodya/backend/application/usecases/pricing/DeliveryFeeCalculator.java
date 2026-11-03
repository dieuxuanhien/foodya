package com.foodya.backend.application.usecases.pricing;

import com.foodya.backend.application.ports.in.DeliveryFeeModifier;
import com.foodya.backend.domain.value_objects.DeliveryFeeBreakdown;
import com.foodya.backend.domain.value_objects.DeliveryFeeContext;
import com.foodya.backend.domain.value_objects.FeeComponent;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Runs the fee modifier chain in order and applies the platform cap.
 *
 * <p>Why a chain rather than one formula: each concern (distance, weather, demand) changes for different
 * reasons and at different cadences. A new modifier is a new class; nothing existing changes.
 *
 * <p>Capping: when the summed total exceeds {@code pricing.max_delivery_fee}, the excess is removed from the
 * last-applied surcharge component(s) first, then from the distance charge, so the breakdown always adds up
 * to the returned total.
 */
public class DeliveryFeeCalculator {

    private final List<DeliveryFeeModifier> modifiers;
    private final PricingSettings settings;

    public DeliveryFeeCalculator(List<DeliveryFeeModifier> modifiers, PricingSettings settings) {
        this.modifiers = modifiers.stream().sorted(Comparator.comparingInt(DeliveryFeeModifier::order)).toList();
        this.settings = settings;
    }

    public DeliveryFeeBreakdown calculate(DeliveryFeeContext context) {
        DeliveryFeeBreakdown current = new DeliveryFeeBreakdown(BigDecimal.ZERO, List.of());
        for (DeliveryFeeModifier modifier : modifiers) {
            current = modifier.apply(current, context);
        }
        return capAtMaximum(current);
    }

    private DeliveryFeeBreakdown capAtMaximum(DeliveryFeeBreakdown breakdown) {
        BigDecimal cap = settings.maxDeliveryFee();
        if (cap == null || breakdown.totalFee().compareTo(cap) <= 0) {
            return breakdown;
        }
        BigDecimal excess = breakdown.totalFee().subtract(cap);
        List<FeeComponent> components = new ArrayList<>(breakdown.components());
        // Trim from the end (surcharges) first, then the distance charge, then base.
        int[] trimOrder = trimPriority(components);
        for (int index : trimOrder) {
            if (excess.signum() <= 0) break;
            FeeComponent component = components.get(index);
            BigDecimal reducible = component.amount().max(BigDecimal.ZERO);
            BigDecimal take = reducible.min(excess);
            if (take.signum() > 0) {
                components.set(index, new FeeComponent(component.name(), component.label(),
                        component.amount().subtract(take)));
                excess = excess.subtract(take);
            }
        }
        return new DeliveryFeeBreakdown(cap, components);
    }

    private static int[] trimPriority(List<FeeComponent> components) {
        List<Integer> order = new ArrayList<>();
        for (int i = components.size() - 1; i >= 0; i--) {
            String name = components.get(i).name();
            if (!name.equals("BASE") && !name.equals("DISTANCE")) {
                order.add(i);
            }
        }
        for (int i = 0; i < components.size(); i++) {
            if (components.get(i).name().equals("DISTANCE")) order.add(i);
        }
        return order.stream().mapToInt(Integer::intValue).toArray();
    }
}
