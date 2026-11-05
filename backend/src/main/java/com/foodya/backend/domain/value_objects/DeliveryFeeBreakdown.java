package com.foodya.backend.domain.value_objects;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** Final delivery fee plus the ordered list of components that produced it. */
public record DeliveryFeeBreakdown(BigDecimal totalFee, List<FeeComponent> components) {

    public DeliveryFeeBreakdown {
        components = List.copyOf(components);
    }

    public static DeliveryFeeBreakdown single(String name, String label, BigDecimal amount) {
        return new DeliveryFeeBreakdown(amount, List.of(new FeeComponent(name, label, amount)));
    }

    public BigDecimal componentAmount(String name) {
        return components.stream()
                .filter(c -> c.name().equals(name))
                .map(FeeComponent::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Sum of every multiplier surcharge (everything except BASE/DISTANCE). */
    public DeliveryFeeBreakdown withComponents(List<FeeComponent> all) {
        return new DeliveryFeeBreakdown(totalFee, new ArrayList<>(all));
    }
}
