package com.foodya.backend.domain.value_objects;

/**
 * Result of a batch cycle execution — statistics for logging/monitoring.
 */
public record BatchCycleResult(
        int batchesCreated,
        int ordersAssigned,
        int singleAssigned,
        int droppedOrders,
        long solverTimeMs
) {
    public static BatchCycleResult empty() {
        return new BatchCycleResult(0, 0, 0, 0, 0);
    }

    public static BatchCycleResult noDrivers() {
        return new BatchCycleResult(0, 0, 0, 0, 0);
    }
}
