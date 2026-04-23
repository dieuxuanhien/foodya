package com.foodya.backend.interfaces.rest.dto;

import java.math.BigDecimal;

public record LocationAddressResponse(
        BigDecimal lat,
        BigDecimal lng,
        String address
) {
}
