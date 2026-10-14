package com.foodya.backend.infrastructure.persistence.models;

import com.foodya.backend.domain.value_objects.BatchItemStatus;
import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "batch_order_items")
public class BatchOrderItemPersistenceModel {

    @Id
    private UUID id;

    @Column(name = "batch_id", nullable = false)
    private UUID batchId;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "pickup_index", nullable = false)
    private int pickupIndex;

    @Column(name = "delivery_index", nullable = false)
    private int deliveryIndex;

    @Column(name = "estimated_pickup_at")
    private OffsetDateTime estimatedPickupAt;

    @Column(name = "estimated_delivery_at")
    private OffsetDateTime estimatedDeliveryAt;

    @Column(name = "actual_pickup_at")
    private OffsetDateTime actualPickupAt;

    @Column(name = "actual_delivery_at")
    private OffsetDateTime actualDeliveryAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_status", nullable = false, length = 20)
    private BatchItemStatus itemStatus;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (id == null) id = UUID.randomUUID();
        if (itemStatus == null) itemStatus = BatchItemStatus.PENDING;
        if (createdAt == null) createdAt = OffsetDateTime.now();
    }

    // Getters and Setters
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

    public OffsetDateTime getEstimatedPickupAt() { return estimatedPickupAt; }
    public void setEstimatedPickupAt(OffsetDateTime estimatedPickupAt) { this.estimatedPickupAt = estimatedPickupAt; }

    public OffsetDateTime getEstimatedDeliveryAt() { return estimatedDeliveryAt; }
    public void setEstimatedDeliveryAt(OffsetDateTime estimatedDeliveryAt) { this.estimatedDeliveryAt = estimatedDeliveryAt; }

    public OffsetDateTime getActualPickupAt() { return actualPickupAt; }
    public void setActualPickupAt(OffsetDateTime actualPickupAt) { this.actualPickupAt = actualPickupAt; }

    public OffsetDateTime getActualDeliveryAt() { return actualDeliveryAt; }
    public void setActualDeliveryAt(OffsetDateTime actualDeliveryAt) { this.actualDeliveryAt = actualDeliveryAt; }

    public BatchItemStatus getItemStatus() { return itemStatus; }
    public void setItemStatus(BatchItemStatus itemStatus) { this.itemStatus = itemStatus; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
