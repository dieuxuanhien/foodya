package com.foodya.backend.application.ports.in;

import com.foodya.backend.application.dto.DriverEarningsView;

import java.time.OffsetDateTime;
import java.util.UUID;

public interface DriverEarningsUseCase {

    /** Earnings for the calling driver between {@code from} and {@code to} (inclusive). */
    DriverEarningsView driverEarnings(UUID driverUserId, OffsetDateTime from, OffsetDateTime to);
}