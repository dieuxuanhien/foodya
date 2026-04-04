package com.foodya.backend.domain.entities;

import com.foodya.backend.application.exception.ValidationException;
import com.foodya.backend.domain.value_objects.CartStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * DOMAIN ENTITY: Cart
 *
 * Represents a customer's shopping cart for a single restaurant.
 * Contains business logic for cart item management, validation, and calculations.
 * BR29: A cart is scoped to a single restaurant.
 */
public class Cart {

    private UUID id;

    private UUID customerUserId;

    private UUID restaurantId;

    private CartStatus status;

    private OffsetDateTime createdAt;

    private OffsetDateTime updatedAt;

    private List<CartItem> items = new ArrayList<>();

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getCustomerUserId() {
        return customerUserId;
    }

    public void setCustomerUserId(UUID customerUserId) {
        this.customerUserId = customerUserId;
    }

    public UUID getRestaurantId() {
        return restaurantId;
    }

    public void setRestaurantId(UUID restaurantId) {
        this.restaurantId = restaurantId;
    }

    public CartStatus getStatus() {
        return status;
    }

    public void setStatus(CartStatus status) {
        this.status = status;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<CartItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    public void setItems(List<CartItem> items) {
        this.items = items != null ? new ArrayList<>(items) : new ArrayList<>();
    }

    /**
     * Validates that a menu item belongs to the cart's restaurant.
     * BR29: Active cart must be scoped to a single restaurant.
     *
     * @param menuItemRestaurantId the restaurant ID of the menu item
     * @throws IllegalStateException if restaurant doesn't match
     */
    public void validateRestaurantScope(UUID menuItemRestaurantId) {
        if (!this.restaurantId.equals(menuItemRestaurantId)) {
            throw new ValidationException(
                "cart restaurant scope violation",
                Map.of(
                    "restaurantId", String.valueOf(restaurantId),
                    "menuItemRestaurantId", String.valueOf(menuItemRestaurantId)
                )
            );
        }
    }

    /**
     * Calculates the total count of items in the cart.
     * @return sum of quantities across all cart items
     */
    public int getTotalItemCount() {
        return items.stream()
                .mapToInt(CartItem::getQuantity)
                .sum();
    }

    /**
     * Calculates the subtotal for all items in the cart.
     * @return sum of all line totals
     */
    public BigDecimal calculateSubtotal() {
        return items.stream()
                .map(CartItem::calculateLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Checks if the cart is empty (no items).
     */
    public boolean isEmpty() {
        return items.isEmpty();
    }

    /**
     * Checks if the cart is active.
     */
    public boolean isActive() {
        return CartStatus.ACTIVE.equals(status);
    }
}
