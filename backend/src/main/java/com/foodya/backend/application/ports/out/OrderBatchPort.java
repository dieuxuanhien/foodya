package com.foodya.backend.application.ports.out;

import com.foodya.backend.domain.entities.BatchOrderItem;
import com.foodya.backend.domain.entities.OrderBatch;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Outbound port for persisting and querying order batches.
 */
public interface OrderBatchPort {

    OrderBatch save(OrderBatch batch);

    Optional<OrderBatch> findById(UUID batchId);

    /**
     * Find the currently active (PENDING or ACTIVE) batch for the given driver.
     */
    Optional<OrderBatch> findActiveBatchForDriver(UUID driverUserId);

    /**
     * Find all batches for admin listing, ordered by creation date descending.
     */
    List<OrderBatch> findAllOrderByCreatedAtDesc();

    BatchOrderItem saveBatchItem(BatchOrderItem item);

    List<BatchOrderItem> findBatchItemsByBatchId(UUID batchId);

    Optional<BatchOrderItem> findBatchItemByBatchIdAndOrderId(UUID batchId, UUID orderId);

    /** Marks a batch item as failed without touching the order state machine. */
    BatchOrderItem failBatchItem(UUID batchId, UUID orderId);

    /** Re-evaluates a batch after one of its items changed (completes it when all items are terminal). */
    OrderBatch refreshBatchStatus(UUID batchId);
}
