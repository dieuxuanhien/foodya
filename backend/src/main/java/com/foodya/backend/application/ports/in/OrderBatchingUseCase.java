package com.foodya.backend.application.ports.in;

import com.foodya.backend.domain.entities.BatchOrderItem;
import com.foodya.backend.domain.entities.OrderBatch;
import com.foodya.backend.domain.value_objects.BatchCycleResult;

import java.util.List;
import java.util.UUID;

/**
 * Inbound port for order batching operations.
 */
public interface OrderBatchingUseCase {

    /**
     * Triggers a full batch cycle: collect eligible orders → build distance matrix →
     * solve VRPPD-TW → persist batches → notify drivers/customers.
     */
    BatchCycleResult triggerBatchCycle();

    /**
     * Gets the active batch for the given driver.
     */
    OrderBatch getActiveBatchForDriver(UUID driverUserId);

    /**
     * Gets batch detail by ID.
     */
    OrderBatch getBatchDetail(UUID batchId);

    /**
     * Gets the items (orders) within a batch.
     */
    List<BatchOrderItem> getBatchItems(UUID batchId);

    /**
     * Driver marks a pickup as complete for a specific order in a batch.
     */
    BatchOrderItem markPickupComplete(UUID batchId, UUID orderId, UUID driverUserId);

    /**
     * Driver marks a delivery as complete for a specific order in a batch.
     */
    BatchOrderItem markDeliveryComplete(UUID batchId, UUID orderId, UUID driverUserId);

    /**
     * Gets batch info for a customer's order (ETA, position in batch).
     */
    CustomerBatchInfo getCustomerBatchInfo(UUID orderId, UUID customerUserId);

    /**
     * Gets all batches for admin monitoring.
     */
    List<OrderBatch> getAllBatches();

    /**
     * Customer batch info DTO.
     */
    record CustomerBatchInfo(
            UUID batchId,
            int totalOrdersInBatch,
            int myDeliveryPosition,
            java.time.Instant estimatedDeliveryAt,
            int estimatedMinutesRemaining
    ) {}
}
