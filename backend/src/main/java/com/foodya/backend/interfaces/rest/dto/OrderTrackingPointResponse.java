package com.foodya.backend.interfaces.rest.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record OrderTrackingPointResponse(
        BigDecimal lat,
        BigDecimal lng,
        OffsetDateTime recordedAt
) {
}
