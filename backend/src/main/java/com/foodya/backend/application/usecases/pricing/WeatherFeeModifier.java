package com.foodya.backend.application.usecases.pricing;

import com.foodya.backend.domain.value_objects.WeatherConditions;
import com.foodya.backend.application.ports.in.DeliveryFeeModifier;
import com.foodya.backend.domain.value_objects.DeliveryFeeBreakdown;
import com.foodya.backend.domain.value_objects.DeliveryFeeContext;
import com.foodya.backend.domain.value_objects.FeeComponent;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Rain surcharge. Keyed off the WMO code rather than the broad category so "heavy rain" (65), showers (82)
 * and thunderstorms (95/96/99) can carry different multipliers.
 */
public class WeatherFeeModifier implements DeliveryFeeModifier {

    private final PricingSettings settings;

    public WeatherFeeModifier(PricingSettings settings) {
        this.settings = settings;
    }

    @Override
    public DeliveryFeeBreakdown apply(DeliveryFeeBreakdown current, DeliveryFeeContext context) {
        WeatherConditions weather = context.weather();
        if (weather == null) {
            return current;
        }
        Tier tier = classify(weather);
        if (tier == null) {
            return current;
        }
        BigDecimal multiplier = switch (tier) {
            case RAIN -> settings.rainMultiplier();
            case HEAVY_RAIN -> settings.heavyRainMultiplier();
            case STORM -> settings.stormMultiplier();
        };
        BigDecimal base = current.totalFee();
        BigDecimal surcharge = base.multiply(multiplier.subtract(BigDecimal.ONE)).setScale(0, RoundingMode.HALF_UP);
        if (surcharge.signum() <= 0) {
            return current;
        }
        String pct = multiplier.subtract(BigDecimal.ONE).multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP).toPlainString();
        String label = tier == Tier.STORM ? "Storm surcharge (+" + pct + "%)"
                : tier == Tier.HEAVY_RAIN ? "Heavy rain surcharge (+" + pct + "%)"
                : "Rain surcharge (+" + pct + "%)";
        List<FeeComponent> components = new ArrayList<>(current.components());
        components.add(new FeeComponent("WEATHER_" + tier.name(), label, surcharge));
        return new DeliveryFeeBreakdown(base.add(surcharge), components);
    }

    enum Tier { RAIN, HEAVY_RAIN, STORM }

    static Tier classify(WeatherConditions weather) {
        Integer code = weather.wmoCode();
        if (code != null) {
            return switch (code) {
                case 95, 96, 97, 99 -> Tier.STORM;
                case 65, 67, 82 -> Tier.HEAVY_RAIN;
                case 51, 53, 55, 56, 57, 61, 63, 66, 80, 81 -> Tier.RAIN;
                default -> null;
            };
        }
        // No code (e.g. legacy callers): fall back to the flag/category text.
        return weather.rainyByCategory() ? Tier.RAIN : null;
    }

    @Override
    public String name() {
        return "WEATHER";
    }

    @Override
    public int order() {
        return 2;
    }
}
