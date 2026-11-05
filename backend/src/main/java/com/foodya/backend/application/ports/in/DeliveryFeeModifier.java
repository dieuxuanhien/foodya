package com.foodya.backend.application.ports.in;

import com.foodya.backend.domain.value_objects.DeliveryFeeBreakdown;
import com.foodya.backend.domain.value_objects.DeliveryFeeContext;

/**
 * One step of the delivery-fee chain (Chain of Responsibility).
 * Implementations are pure functions of (current breakdown, context) and must not do I/O.
 */
public interface DeliveryFeeModifier {

    DeliveryFeeBreakdown apply(DeliveryFeeBreakdown current, DeliveryFeeContext context);

    String name();

    /** Applied in ascending order. Distance runs first; multipliers follow. */
    int order();
}
