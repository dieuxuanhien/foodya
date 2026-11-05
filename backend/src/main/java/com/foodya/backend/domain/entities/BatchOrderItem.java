package com.foodya.backend.domain.entities;

import com.foodya.backend.domain.value_objects.BatchItemStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * Domain entity representing a single order within a batch.
 * Pure domain object — no JPA annotations.
 */
public class BatchOrderItem {

    private UUID id;
    private UUID batchId;
    private UUID orderId;
    private int pickupIndex;
    private int deliveryIndex;
    private Instant estimatedPickupAt;
    private Instant estimatedDeliveryAt;
    private Instant actualPickupAt;
    private Instant actualDeliveryAt;
    private BatchItemStatus itemStatus;
    private Instant createdAt;

    public BatchOrderItem() {}

    public static BatchOrderItem create(UUID batchId, UUID orderId,
                                         int pickupIndex, int deliveryIndex,
                                         Instant estimatedPickupAt, Instant estimatedDeliveryAt) {
        BatchOrderItem item = new BatchOrderItem();
        item.id = UUID.randomUUID();
        item.batchId = batchId;
        item.orderId = orderId;
        item.pickupIndex = pickupIndex;
        item.deliveryIndex = deliveryIndex;
        item.estimatedPickupAt = estimatedPickupAt;
        item.estimatedDeliveryAt = estimatedDeliveryAt;
        item.itemStatus = BatchItemStatus.PENDING;
        item.createdAt = Instant.now();
        return item;
    }

    public void markPickedUp() {
        if (itemStatus != BatchItemStatus.PENDING) {
            throw new IllegalStateException("Cannot mark picked up from status: " + itemStatus);
        }
        itemStatus = BatchItemStatus.PICKED_UP;
        actualPickupAt = Instant.now();
    }

    public void markDelivering() {
        if (itemStatus != BatchItemStatus.PICKED_UP) {
            throw new IllegalStateException("Cannot mark delivering from status: " + itemStatus);
        }
        itemStatus = BatchItemStatus.DELIVERING;
    }

    public void markDelivered() {
        if (itemStatus != BatchItemStatus.PICKED_UP && itemStatus != BatchItemStatus.DELIVERING) {
            throw new IllegalStateException("Cannot mark delivered from status: " + itemStatus);
        }
        itemStatus = BatchItemStatus.DELIVERED;
        actualDeliveryAt = Instant.now();
    }

    public void markFailed() {
        itemStatus = BatchItemStatus.FAILED;
    }

    // Getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getBatchId() { return batchId; }
    public void setBatchId(UUID batchId) { this.batchId = batchId; }

    public UUID getOrderId() { return orderId; }
    public void setOrderId(UUID orderId) { this.orderId = orderId; }

    public int getPickupIndex() { return pickupIndex; }
    public void setPickupIndex(int pickupIndex) { this.pickupIndex = pickupIndex; }

    public int getDeliveryIndex() { return deliveryIndex; }
    public void setDeliveryIndex(int deliveryIndex) { this.deliveryIndex = deliveryIndex; }

    public Instant getEstimatedPickupAt() { return estimatedPickupAt; }
    public void setEstimatedPickupAt(Instant estimatedPickupAt) { this.estimatedPickupAt = estimatedPickupAt; }

    public Instant getEstimatedDeliveryAt() { return estimatedDeliveryAt; }
    public void setEstimatedDeliveryAt(Instant estimatedDeliveryAt) { this.estimatedDeliveryAt = estimatedDeliveryAt; }

    public Instant getActualPickupAt() { return actualPickupAt; }
    public void setActualPickupAt(Instant actualPickupAt) { this.actualPickupAt = actualPickupAt; }

    public Instant getActualDeliveryAt() { return actualDeliveryAt; }
    public void setActualDeliveryAt(Instant actualDeliveryAt) { this.actualDeliveryAt = actualDeliveryAt; }

    public BatchItemStatus getItemStatus() { return itemStatus; }
    public void setItemStatus(BatchItemStatus itemStatus) { this.itemStatus = itemStatus; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
