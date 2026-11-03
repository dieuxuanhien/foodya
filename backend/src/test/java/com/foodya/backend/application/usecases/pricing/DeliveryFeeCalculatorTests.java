package com.foodya.backend.application.usecases.pricing;

import com.foodya.backend.application.dto.WeatherData;
import com.foodya.backend.application.ports.in.DeliveryFeeModifier;
import com.foodya.backend.domain.value_objects.DeliveryFeeBreakdown;
import com.foodya.backend.domain.value_objects.DeliveryFeeContext;
import com.foodya.backend.domain.value_objects.WeatherConditions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DeliveryFeeCalculatorTests {

    private static DeliveryFeeCalculator calculator(PricingSettings settings) {
        return new DeliveryFeeCalculator(
                List.of(new DemandSurgeFeeModifier(settings), new WeatherFeeModifier(settings),
                        new DistanceFeeModifier(settings)), settings);
    }

    private static WeatherConditions w(WeatherData d) {
        return new WeatherConditions(d.weatherCode(), d.weatherMain(), d.isRaining());
    }

    private static DeliveryFeeContext ctx(String km, WeatherConditions weather, double ratio) {
        return new DeliveryFeeContext(new BigDecimal(km), weather, LocalDateTime.of(2026, 11, 4, 12, 0),
                ratio, 10.76, 106.66);
    }

    @Test
    void baseFeeOnlyWithinBaseDistance() {
        var result = calculator(PricingSettings.defaults()).calculate(ctx("1.5", null, 0));
        assertThat(result.totalFee()).isEqualByComparingTo("10000");
        assertThat(result.componentAmount("DISTANCE")).isEqualByComparingTo("0");
    }

    @Test
    void addsPerKmBeyondBaseDistance() {
        // 6 km => base 10,000 + (6-2)*5,000 = 30,000
        var result = calculator(PricingSettings.defaults()).calculate(ctx("6.0", null, 0));
        assertThat(result.totalFee()).isEqualByComparingTo("30000");
        assertThat(result.componentAmount("DISTANCE")).isEqualByComparingTo("20000");
    }

    @Test
    void rainSurchargeAppliesMultiplier() {
        // 30,000 * 15% = 4,500
        var rain = WeatherData.of("Rain", "Slight rain", 27.0);
        var result = calculator(PricingSettings.defaults()).calculate(ctx("6.0", w(rain), 0));
        assertThat(result.componentAmount("WEATHER_RAIN")).isEqualByComparingTo("4500");
        assertThat(result.totalFee()).isEqualByComparingTo("34500");
    }

    @Test
    void heavyRainAndStormUseTheirOwnMultipliers() {
        var heavy = new WeatherData("Rain", "Heavy rain", 26.0, 90, 8.0, 12.0, 65, true);
        var storm = new WeatherData("Thunderstorm", "Thunderstorm", 25.0, 95, 15.0, 20.0, 95, true);
        var calc = calculator(PricingSettings.defaults());

        // 30,000 * 30% = 9,000
        assertThat(calc.calculate(ctx("6.0", w(heavy), 0)).totalFee()).isEqualByComparingTo("39000");
        // 30,000 * 50% = 15,000
        assertThat(calc.calculate(ctx("6.0", w(storm), 0)).totalFee()).isEqualByComparingTo("45000");
    }

    @Test
    void clearAndSnowDoNotTriggerWeatherSurcharge() {
        var clear = new WeatherData("Clear", "Clear sky", 33.0, 50, 2.0, 0.0, 0, false);
        var snow = new WeatherData("Snow", "Moderate snowfall", 2.0, 80, 3.0, 0.0, 73, false);
        var calc = calculator(PricingSettings.defaults());

        assertThat(calc.calculate(ctx("6.0", w(clear), 0)).totalFee()).isEqualByComparingTo("30000");
        assertThat(calc.calculate(ctx("6.0", w(snow), 0)).totalFee()).isEqualByComparingTo("30000");
    }

    @Test
    void surgeIsIgnoredWhenDisabled() {
        var result = calculator(PricingSettings.defaults()).calculate(ctx("6.0", null, 5.0));
        assertThat(result.totalFee()).isEqualByComparingTo("30000");
    }

    @Test
    void surgeAppliesWhenEnabledAndRatioExceeded() {
        PricingSettings defaults = PricingSettings.defaults();
        PricingSettings enabled = new PricingSettings(defaults.baseFee(), defaults.baseDistanceKm(),
                defaults.feePerKm(), defaults.maxDeliveryFee(), defaults.rainMultiplier(),
                defaults.heavyRainMultiplier(), defaults.stormMultiplier(), true,
                defaults.surgeThresholdRatio(), defaults.surgeMultiplier(), defaults.surgeMaxMultiplier());
        // 30,000 * 20% = 6,000
        var result = calculator(enabled).calculate(ctx("6.0", null, 3.0));
        assertThat(result.componentAmount("SURGE")).isEqualByComparingTo("6000");
        assertThat(result.totalFee()).isEqualByComparingTo("36000");
    }

    @Test
    void totalIsCappedAndBreakdownStillReconciles() {
        var storm = new WeatherData("Thunderstorm", "Thunderstorm", 25.0, 95, 15.0, 20.0, 95, true);
        // 20 km => 10,000 + 18*5,000 = 100,000; +50% storm = 150,000; cap 100,000.
        var result = calculator(PricingSettings.defaults()).calculate(ctx("20.0", w(storm), 0));
        assertThat(result.totalFee()).isEqualByComparingTo("100000");
        BigDecimal sum = result.components().stream().map(c -> c.amount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(sum).isEqualByComparingTo("100000");
    }

    @Test
    void weatherWithoutCodeFallsBackToCategoryText() {
        var legacy = WeatherData.of("Rain", "rain", 27.0);
        var result = calculator(PricingSettings.defaults()).calculate(ctx("6.0", w(legacy), 0));
        assertThat(result.componentAmount("WEATHER_RAIN")).isEqualByComparingTo("4500");
    }

    @Test
    void modifiersRunInOrder() {
        var modifiers = List.of(new WeatherFeeModifier(PricingSettings.defaults()),
                new DistanceFeeModifier(PricingSettings.defaults()));
        var calc = new DeliveryFeeCalculator(modifiers, PricingSettings.defaults());
        // Distance must run first so weather applies to the distance-inclusive fee.
        var rain = WeatherData.of("Rain", "Slight rain", 27.0);
        assertThat(calc.calculate(ctx("6.0", w(rain), 0)).totalFee()).isEqualByComparingTo("34500");
    }

    @Test
    void noModifiersYieldsZero() {
        var calc = new DeliveryFeeCalculator(List.of(), PricingSettings.defaults());
        assertThat(calc.calculate(ctx("5.0", null, 0)).totalFee()).isEqualByComparingTo("0");
    }

    @Test
    void modifierNamesAndOrdersAreDistinct() {
        var settings = PricingSettings.defaults();
        List<DeliveryFeeModifier> mods = List.of(new DistanceFeeModifier(settings),
                new WeatherFeeModifier(settings), new DemandSurgeFeeModifier(settings));
        assertThat(mods.stream().map(DeliveryFeeModifier::order).distinct().count()).isEqualTo(3);
        assertThat(mods.stream().map(DeliveryFeeModifier::name).distinct().count()).isEqualTo(3);
    }

    @Test
    void breakdownSingleFactory() {
        DeliveryFeeBreakdown b = DeliveryFeeBreakdown.single("BASE", "Base", BigDecimal.TEN);
        assertThat(b.totalFee()).isEqualByComparingTo("10");
        assertThat(b.components()).hasSize(1);
    }
}
