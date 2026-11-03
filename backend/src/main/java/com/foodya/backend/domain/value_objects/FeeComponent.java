package com.foodya.backend.domain.value_objects;

import java.math.BigDecimal;

/**
 * One line of the fee breakdown shown to the customer.
 *
 * @param name        stable identifier (e.g. "BASE", "DISTANCE", "WEATHER_RAIN", "SURGE")
 * @param label       customer-facing label ("Rain surcharge (+15%)")
 * @param amount      signed amount in currency units; negative for discounts
 */
public record FeeComponent(String name, String label, BigDecimal amount) {}
