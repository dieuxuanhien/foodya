package com.foodya.backend.domain.value_objects;

/**
 * Represents the lifecycle state of a driver's online session.
 * <p>
 * State machine (Grab-style):
 * <pre>
 *   OFFLINE ←→ ONLINE → ON_DELIVERY → ONLINE (auto, after delivery completes)
 *   ONLINE  → BUSY (max orders reached) → ONLINE (auto, when order delivered)
 *   Any     → SUSPENDED (admin action) → OFFLINE (after unsuspend)
 *   "Last trip" mode: GOING_OFFLINE — driver taps offline while ON_DELIVERY;
 *                     system finishes current order then auto-transitions to OFFLINE.
 * </pre>
 */
public enum DriverSessionStatus {
    /** Driver has toggled ON and is accepting orders. */
    ONLINE,
    /** Driver is currently delivering one or more orders. */
    ON_DELIVERY,
    /** Driver has reached max concurrent orders and cannot accept more. */
    BUSY,
    /** Driver tapped "offline" while mid-delivery. Finishes current order, then auto-OFFLINE. */
    GOING_OFFLINE,
    /** Session closed — driver toggled OFF or auto-transitioned after last delivery. */
    OFFLINE,
    /** Admin has suspended this driver — cannot go online until unsuspended. */
    SUSPENDED
}
