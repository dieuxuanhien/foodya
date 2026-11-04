package com.foodya.backend.interfaces.rest.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Body of the admin "Done" click. {@code expectedNetAmount} is the amount the admin saw on screen; if the
 * balance moved since (another order completed), the request is rejected with 409 so nobody pays a number
 * they did not review.
 */
public record SettleSettlementApiRequest(
        @DecimalMin("0") BigDecimal expectedNetAmount,
        @Size(max = 500) String note
) {}