package com.foodya.backend.domain.entities;

import com.foodya.backend.domain.value_objects.BatchStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * Domain entity representing a batch of orders assigned to a single driver.
 * Pure domain object — no JPA annotations.
 */
public class OrderBatch {

    private UUID id;
    private UUID driverUserId;
    private UUID sessionId;
    private BatchStatus status;
    private String pickupSequence;       // JSON string
    private String deliverySequence;     // JSON string
    private Integer totalDistanceMeters;
    private Integer totalDurationSeconds;
    private String optimizedRoutePolyline;
    private String solverMetadata;       // JSON string
    private Instant createdAt;
    private Instant startedAt;
    private Instant completedAt;

    public OrderBatch() {}

    /**
     * Creates a new batch from VRP solver output.
     */
    public static OrderBatch create(UUID driverUserId, UUID sessionId,
                                     String pickupSequence, String deliverySequence,
                                     Integer totalDistanceMeters, Integer totalDurationSeconds,
                                     String solverMetadata) {
        OrderBatch batch = new OrderBatch();
        batch.id = UUID.randomUUID();
        batch.driverUserId = driverUserId;
        batch.sessionId = sessionId;
        batch.status = BatchStatus.PENDING;
        batch.pickupSequence = pickupSequence;
        batch.deliverySequence = deliverySequence;
        batch.totalDistanceMeters = totalDistanceMeters;
        batch.totalDurationSeconds = totalDurationSeconds;
        batch.solverMetadata = solverMetadata;
        batch.createdAt = Instant.now();
        return batch;
    }

    public void activate() {
        if (status != BatchStatus.PENDING) {
            throw new IllegalStateException("Cannot activate batch from status: " + status);
        }
        status = BatchStatus.ACTIVE;
        startedAt = Instant.now();
    }

    public void complete() {
        if (status != BatchStatus.ACTIVE) {
            throw new IllegalStateException("Cannot complete batch from status: " + status);
        }
        status = BatchStatus.COMPLETED;
        completedAt = Instant.now();
    }

    public void markPartial() {
        if (status != BatchStatus.ACTIVE) {
            throw new IllegalStateException("Cannot mark partial from status: " + status);
        }
        status = BatchStatus.PARTIAL;
        completedAt = Instant.now();
    }

    public void cancel() {
        if (status != BatchStatus.PENDING && status != BatchStatus.ACTIVE) {
            throw new IllegalStateException("Cannot cancel batch from status: " + status);
        }
        status = BatchStatus.CANCELLED;
        completedAt = Instant.now();
    }

    // Getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getDriverUserId() { return driverUserId; }
    public void setDriverUserId(UUID driverUserId) { this.driverUserId = driverUserId; }

    public UUID getSessionId() { return sessionId; }
    public void setSessionId(UUID sessionId) { this.sessionId = sessionId; }

    public BatchStatus getStatus() { return status; }
    public void setStatus(BatchStatus status) { this.status = status; }

    public String getPickupSequence() { return pickupSequence; }
    public void setPickupSequence(String pickupSequence) { this.pickupSequence = pickupSequence; }

    public String getDeliverySequence() { return deliverySequence; }
    public void setDeliverySequence(String deliverySequence) { this.deliverySequence = deliverySequence; }

    public Integer getTotalDistanceMeters() { return totalDistanceMeters; }
    public void setTotalDistanceMeters(Integer totalDistanceMeters) { this.totalDistanceMeters = totalDistanceMeters; }

    public Integer getTotalDurationSeconds() { return totalDurationSeconds; }
    public void setTotalDurationSeconds(Integer totalDurationSeconds) { this.totalDurationSeconds = totalDurationSeconds; }

    public String getOptimizedRoutePolyline() { return optimizedRoutePolyline; }
    public void setOptimizedRoutePolyline(String optimizedRoutePolyline) { this.optimizedRoutePolyline = optimizedRoutePolyline; }

    public String getSolverMetadata() { return solverMetadata; }
    public void setSolverMetadata(String solverMetadata) { this.solverMetadata = solverMetadata; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }

    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
}
