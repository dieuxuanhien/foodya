package com.foodya.backend.application.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record OrderTrackingPointView(
        BigDecimal lat,
        BigDecimal lng,
        OffsetDateTime recordedAt
) {
}
