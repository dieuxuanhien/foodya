package com.foodya.backend.domain.value_objects;

/**
 * Statuses a driver session is considered "live" in: the driver is on shift and may be matched.
 */
public final class DriverSessionLifecycle {

    private DriverSessionLifecycle() {}

    public static boolean isLive(DriverSessionStatus status) {
        return status == DriverSessionStatus.ONLINE
                || status == DriverSessionStatus.ON_DELIVERY
                || status == DriverSessionStatus.BUSY
                || status == DriverSessionStatus.GOING_OFFLINE;
    }
}