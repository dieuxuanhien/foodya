package com.foodya.backend.domain.catalog;

import com.foodya.backend.domain.value_objects.ParameterValueType;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class SystemParameterCatalog {

    private SystemParameterCatalog() {
    }

    public static Map<String, ParameterRule> defaults() {
        Map<String, ParameterRule> rules = new LinkedHashMap<>();
        rules.put("shipping.base_delivery_fee", ParameterRule.number("10000", true, new NumberConstraint(BigDecimal.ZERO, null)));
        rules.put("shipping.base_distance_km", ParameterRule.number("2.0", true, new NumberConstraint(BigDecimal.ZERO, null)));
        rules.put("shipping.fee_per_km", ParameterRule.number("5000", true, new NumberConstraint(BigDecimal.ZERO, null)));
        rules.put("shipping.max_delivery_km", ParameterRule.number("15.0", true, new NumberConstraint(BigDecimal.ZERO, null, true)));

        rules.put("search.nearby.max_radius_km", ParameterRule.number("10.0", true, new NumberConstraint(BigDecimal.ZERO, null, true)));
        rules.put("search.default_page_size", ParameterRule.number("20", true, new NumberConstraint(BigDecimal.ONE, BigDecimal.valueOf(100))));
        rules.put("search.max_page_size", ParameterRule.number("100", true, new NumberConstraint(BigDecimal.ONE, BigDecimal.valueOf(200))));
        rules.put("catalog.menu_item_price_min", ParameterRule.number("1000", true, new NumberConstraint(BigDecimal.ZERO, null, true)));
        rules.put("catalog.menu_item_price_max", ParameterRule.number("10000000", true, new NumberConstraint(BigDecimal.ONE, null, true)));

        rules.put("finance.commission_rate_percent", ParameterRule.number("10.0", true, new NumberConstraint(BigDecimal.ZERO, BigDecimal.valueOf(100))));
        rules.put("finance.shipping_margin_rate_percent", ParameterRule.number("0.0", true, new NumberConstraint(BigDecimal.ZERO, BigDecimal.valueOf(100))));

        rules.put("currency.code", ParameterRule.string("VND", false, value -> {
            if (!value.matches("^[A-Z]{3}$")) {
                throw new IllegalArgumentException("must be ISO-4217 uppercase code");
            }
        }));
        rules.put("currency.minor_unit", ParameterRule.number("0", false, new NumberConstraint(BigDecimal.ZERO, BigDecimal.valueOf(4))));
        rules.put("currency.rounding_mode", ParameterRule.string("HALF_UP", false, value -> {
            Set<String> allowed = Set.of("HALF_UP", "HALF_EVEN", "UP", "DOWN");
            if (!allowed.contains(value.toUpperCase(Locale.ROOT))) {
                throw new IllegalArgumentException("must be one of HALF_UP, HALF_EVEN, UP, DOWN");
            }
        }));

        rules.put("retention.customer_data_days", ParameterRule.number("365", false, new NumberConstraint(BigDecimal.valueOf(30), null)));
        rules.put("retention.order_history_days", ParameterRule.number("730", false, new NumberConstraint(BigDecimal.valueOf(30), null)));
        rules.put("retention.audit_logs_days", ParameterRule.number("365", false, new NumberConstraint(BigDecimal.valueOf(30), null)));
        rules.put("retention.tracking_points_days", ParameterRule.number("30", false, new NumberConstraint(BigDecimal.valueOf(30), null)));
        rules.put("retention.ai_chat_days", ParameterRule.number("90", false, new NumberConstraint(BigDecimal.valueOf(30), null)));
        rules.put("ops.backup.rpo_minutes", ParameterRule.number("15", false, new NumberConstraint(BigDecimal.ONE, BigDecimal.valueOf(15))));
        rules.put("ops.backup.rto_minutes", ParameterRule.number("60", false, new NumberConstraint(BigDecimal.ONE, BigDecimal.valueOf(60))));

        // Order batching (Phase 5) — read at runtime by OrderBatchingService.
        rules.put("batching.enabled", ParameterRule.bool("true", true));
        rules.put("batching.cooldown_seconds", ParameterRule.number("10", true, new NumberConstraint(BigDecimal.ZERO, null)));
        rules.put("batching.max_wait_for_batch_seconds", ParameterRule.number("120", true, new NumberConstraint(BigDecimal.ONE, null)));
        rules.put("batching.search_radius_km", ParameterRule.number("10.0", true, new NumberConstraint(BigDecimal.ZERO, null, true)));
        rules.put("batching.pickup_service_time_seconds", ParameterRule.number("300", true, new NumberConstraint(BigDecimal.ZERO, null)));
        rules.put("batching.delivery_service_time_seconds", ParameterRule.number("180", true, new NumberConstraint(BigDecimal.ZERO, null)));
        rules.put("batching.max_delivery_time_minutes", ParameterRule.number("45", true, new NumberConstraint(BigDecimal.ONE, null)));
        rules.put("batching.solver_time_limit_ms", ParameterRule.number("3000", true, new NumberConstraint(BigDecimal.valueOf(100), null)));
        return rules;
    }

    public record ParameterRule(
            ParameterValueType type,
            String defaultValue,
            boolean runtimeApplicable,
            ValueValidator validator
    ) {
        public static ParameterRule number(String defaultValue, boolean runtimeApplicable, NumberConstraint constraint) {
            return new ParameterRule(ParameterValueType.NUMBER, defaultValue, runtimeApplicable, constraint);
        }

        public static ParameterRule string(String defaultValue, boolean runtimeApplicable, ValueValidator validator) {
            return new ParameterRule(ParameterValueType.STRING, defaultValue, runtimeApplicable, validator);
        }

        public static ParameterRule bool(String defaultValue, boolean runtimeApplicable) {
            return new ParameterRule(ParameterValueType.BOOLEAN, defaultValue, runtimeApplicable, value -> {
                if (!"true".equalsIgnoreCase(value) && !"false".equalsIgnoreCase(value)) {
                    throw new IllegalArgumentException("must be true or false");
                }
            });
        }
    }

    @FunctionalInterface
    public interface ValueValidator {
        void validate(String value);
    }

    public record NumberConstraint(BigDecimal minInclusive, BigDecimal maxInclusive) implements ValueValidator {
        public NumberConstraint(BigDecimal minInclusive, BigDecimal maxInclusive, boolean exclusiveMinZero) {
            this(exclusiveMinZero ? new BigDecimal("0.000000001") : minInclusive, maxInclusive);
        }

        @Override
        public void validate(String value) {
            BigDecimal number;
            try {
                number = new BigDecimal(value);
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException("must be a valid number");
            }

            if (minInclusive != null && number.compareTo(minInclusive) < 0) {
                throw new IllegalArgumentException("must be >= " + minInclusive.toPlainString());
            }
            if (maxInclusive != null && number.compareTo(maxInclusive) > 0) {
                throw new IllegalArgumentException("must be <= " + maxInclusive.toPlainString());
            }
        }
    }
}
