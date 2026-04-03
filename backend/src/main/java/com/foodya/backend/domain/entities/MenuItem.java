package com.foodya.backend.domain.entities;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * DOMAIN ENTITY: MenuItem
 *
 * Represents a menu item offered by a restaurant.
 * Contains business logic for orderability validation and price management.
 */
public class MenuItem {

    private UUID id;

    private UUID restaurantId;

    private UUID categoryId;

    private String name;

    private String description;

    private BigDecimal price;

    private boolean active;

    private boolean available;

    private OffsetDateTime deletedAt;

    private OffsetDateTime createdAt;

    private OffsetDateTime updatedAt;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getRestaurantId() {
        return restaurantId;
    }

    public void setRestaurantId(UUID restaurantId) {
        this.restaurantId = restaurantId;
    }

    public UUID getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(UUID categoryId) {
        this.categoryId = categoryId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public OffsetDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(OffsetDateTime deletedAt) {
        this.deletedAt = deletedAt;
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

    /**
     * Checks if this menu item is orderable by customers.
     * A menu item is orderable if it's active, available, and not soft-deleted.
     *
     * @return true if orderable, false otherwise
     */
    public boolean isOrderable() {
        return active && available && deletedAt == null;
    }

    /**
     * Updates the price with validation.
     *
     * @param newPrice the new price (must be positive)
     * @throws IllegalArgumentException if price is not positive
     */
    public void updatePrice(BigDecimal newPrice) {
        if (newPrice == null || newPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("price must be positive");
        }
        this.price = newPrice;
    }

    /**
     * Marks this menu item as deleted (soft delete).
     */
    public void softDelete() {
        if (this.deletedAt == null) {
            this.deletedAt = OffsetDateTime.now();
            this.active = false;
            this.available = false;
        }
    }

    /**
     * Marks this menu item as unavailable (e.g., out of stock).
     */
    public void markUnavailable() {
        this.available = false;
    }

    /**
     * Marks this menu item as available.
     */
    public void markAvailable() {
        if (deletedAt == null && active) {
            this.available = true;
        }
    }
}
