package com.foodya.backend.domain.entities;

import com.foodya.backend.domain.value_objects.DriverSessionStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * Domain entity representing a single driver online session (internal — drivers see only the GO toggle).
 * <p>
 * Each OFFLINE → ONLINE transition creates a new DriverOnlineSession record.
 * When the driver goes offline the session is closed by setting {@code endedAt}.
 * <p>
 * This entity contains no JPA or framework annotations — it is a pure domain object.
 */
public class DriverOnlineSession {

    private UUID id;
    private UUID driverUserId;
    private DriverSessionStatus status;
    private Instant startedAt;
    private Instant endedAt;
    private Instant lastHeartbeat;
    private String h3IndexRes9;
    private Double currentLat;
    private Double currentLng;
    private int activeOrderCount;
    private int maxConcurrentOrders;
    private Instant createdAt;

    public DriverOnlineSession() {
    }

    // -------------------------------------------------------------------------
    // Domain state-machine methods
    // -------------------------------------------------------------------------

    /**
     * Driver taps the GO button for the first time (OFFLINE → ONLINE).
     * Initialises a fresh session — caller must save the returned entity.
     */
    public void goOnline(UUID driverUserId) {
        this.driverUserId = driverUserId;
        this.status = DriverSessionStatus.ONLINE;
        this.startedAt = Instant.now();
        this.lastHeartbeat = Instant.now();
        this.activeOrderCount = 0;
        this.maxConcurrentOrders = 2; // default; can be overridden by admin
        this.createdAt = Instant.now();
    }

    /**
     * Driver taps the GO button again to go offline.
     * <ul>
     *   <li>If ONLINE or BUSY → immediately sets status OFFLINE and stamps {@code endedAt}.</li>
     *   <li>If ON_DELIVERY → transitions to GOING_OFFLINE ("last trip" mode).</li>
     *   <li>Any other state → {@link IllegalStateException}.</li>
     * </ul>
     */
    public void requestOffline() {
        switch (status) {
            case ONLINE, BUSY -> {
                status = DriverSessionStatus.OFFLINE;
                endedAt = Instant.now();
            }
            case ON_DELIVERY -> status = DriverSessionStatus.GOING_OFFLINE;
            default -> throw new IllegalStateException(
                    "Cannot request offline from status: " + status);
        }
    }

    /**
     * Called when the driver is assigned a new delivery order.
     * Increments {@code activeOrderCount} and transitions to ON_DELIVERY when first order picked up.
     * Validates that the driver can still accept orders.
     */
    public void startDelivery() {
        if (status != DriverSessionStatus.ONLINE && status != DriverSessionStatus.ON_DELIVERY) {
            throw new IllegalStateException(
                    "Cannot start delivery from status: " + status + ". Must be ONLINE or ON_DELIVERY.");
        }
        if (activeOrderCount >= maxConcurrentOrders) {
            throw new IllegalStateException(
                    "Driver has reached max concurrent orders: " + maxConcurrentOrders);
        }
        activeOrderCount++;
        if (status == DriverSessionStatus.ONLINE) {
            status = DriverSessionStatus.ON_DELIVERY;
        }
        // If already ON_DELIVERY (batching), just increment the counter
        if (activeOrderCount >= maxConcurrentOrders) {
            status = DriverSessionStatus.BUSY;
        }
        lastHeartbeat = Instant.now();
    }

    /**
     * Called when a delivery order is completed (SUCCESS or FAILED).
     * Decrements {@code activeOrderCount} and determines next state:
     * <ul>
     *   <li>Count reaches 0 and GOING_OFFLINE → auto-OFFLINE (closes session)</li>
     *   <li>Count reaches 0 and BUSY/ON_DELIVERY → back to ONLINE</li>
     *   <li>Count still > 0 → stay in current state (or transition from BUSY to ON_DELIVERY)</li>
     * </ul>
     */
    public void completeDelivery() {
        if (status != DriverSessionStatus.ON_DELIVERY
                && status != DriverSessionStatus.GOING_OFFLINE
                && status != DriverSessionStatus.BUSY) {
            throw new IllegalStateException(
                    "Cannot complete delivery from status: " + status);
        }
        activeOrderCount = Math.max(0, activeOrderCount - 1);
        if (activeOrderCount == 0) {
            if (status == DriverSessionStatus.GOING_OFFLINE) {
                status = DriverSessionStatus.OFFLINE;
                endedAt = Instant.now();
            } else {
                status = DriverSessionStatus.ONLINE;
            }
        } else {
            // Still has orders — if was BUSY and now below max, go back to ON_DELIVERY
            if (status == DriverSessionStatus.BUSY) {
                status = DriverSessionStatus.ON_DELIVERY;
            }
        }
        lastHeartbeat = Instant.now();
    }

    /**
     * Updates the driver's current GPS position and H3 cell.
     * Also refreshes the heartbeat timestamp.
     */
    public void updateLocation(double lat, double lng, String h3Index) {
        this.currentLat = lat;
        this.currentLng = lng;
        this.h3IndexRes9 = h3Index;
        this.lastHeartbeat = Instant.now();
    }

    /**
     * Returns true if this driver can accept a new order right now.
     */
    public boolean canAcceptOrder() {
        return (status == DriverSessionStatus.ONLINE || status == DriverSessionStatus.ON_DELIVERY)
                && activeOrderCount < maxConcurrentOrders;
    }

    /**
     * Admin suspends the driver. Can be called from any active state.
     */
    public void suspend() {
        if (status == DriverSessionStatus.OFFLINE || status == DriverSessionStatus.SUSPENDED) {
            throw new IllegalStateException("Driver is already offline or suspended.");
        }
        status = DriverSessionStatus.SUSPENDED;
    }

    /**
     * Admin unsuspends the driver — transitions back to OFFLINE (driver must tap GO again).
     */
    public void unsuspend() {
        if (status != DriverSessionStatus.SUSPENDED) {
            throw new IllegalStateException("Driver is not suspended.");
        }
        status = DriverSessionStatus.OFFLINE;
        endedAt = Instant.now();
    }

    // -------------------------------------------------------------------------
    // Getters and setters (no annotations — pure domain)
    // -------------------------------------------------------------------------

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getDriverUserId() { return driverUserId; }
    public void setDriverUserId(UUID driverUserId) { this.driverUserId = driverUserId; }

    public DriverSessionStatus getStatus() { return status; }
    public void setStatus(DriverSessionStatus status) { this.status = status; }

    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }

    public Instant getEndedAt() { return endedAt; }
    public void setEndedAt(Instant endedAt) { this.endedAt = endedAt; }

    public Instant getLastHeartbeat() { return lastHeartbeat; }
    public void setLastHeartbeat(Instant lastHeartbeat) { this.lastHeartbeat = lastHeartbeat; }

    public String getH3IndexRes9() { return h3IndexRes9; }
    public void setH3IndexRes9(String h3IndexRes9) { this.h3IndexRes9 = h3IndexRes9; }

    public Double getCurrentLat() { return currentLat; }
    public void setCurrentLat(Double currentLat) { this.currentLat = currentLat; }

    public Double getCurrentLng() { return currentLng; }
    public void setCurrentLng(Double currentLng) { this.currentLng = currentLng; }

    public int getActiveOrderCount() { return activeOrderCount; }
    public void setActiveOrderCount(int activeOrderCount) { this.activeOrderCount = activeOrderCount; }

    public int getMaxConcurrentOrders() { return maxConcurrentOrders; }
    public void setMaxConcurrentOrders(int maxConcurrentOrders) { this.maxConcurrentOrders = maxConcurrentOrders; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
