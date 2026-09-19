package com.foodya.backend.application.ports.in;

import com.foodya.backend.application.dto.DriverSessionView;

import java.util.UUID;

/**
 * Inbound port defining the driver lifecycle use cases exposed to the REST layer.
 * Corresponds to the Grab-style GO button interactions.
 */
public interface DriverLifecycleUseCase {

    /**
     * Driver taps the GO button (OFFLINE → ONLINE).
     * Creates a new online session for the driver.
     * Throws {@link com.foodya.backend.application.exception.ValidationException} if already online.
     *
     * @param driverUserId the authenticated driver's user ID
     * @return the new active session view
     */
    DriverSessionView toggleOnline(UUID driverUserId);

    /**
     * Driver taps the GO button again (ONLINE → OFFLINE or ON_DELIVERY → GOING_OFFLINE).
     * If mid-delivery, enters "last trip" mode: finishes current order, then auto-goes offline.
     * Throws {@link com.foodya.backend.application.exception.ValidationException} if already offline.
     *
     * @param driverUserId the authenticated driver's user ID
     * @return the updated session view
     */
    DriverSessionView toggleOffline(UUID driverUserId);

    /**
     * Driver sends a location update (lat/lng). Updates the session's current position and heartbeat.
     * Also computes and stores the H3 res9 cell for spatial lookups.
     *
     * @param driverUserId the authenticated driver's user ID
     * @param lat          latitude (-90 to 90)
     * @param lng          longitude (-180 to 180)
     * @return the updated session view
     */
    DriverSessionView updateLocation(UUID driverUserId, double lat, double lng);

    /**
     * Returns the current session view for the driver.
     * Returns an OFFLINE view if no active session exists.
     *
     * @param driverUserId the authenticated driver's user ID
     * @return the current session view
     */
    DriverSessionView getActiveSession(UUID driverUserId);
}
