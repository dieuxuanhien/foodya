package com.foodya.backend.domain.entities;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DOMAIN ENTITY: CartItem
 *
 * Represents a single line item in a customer's shopping cart.
 * Contains business logic for quantity management and line total calculation.
 */
public class CartItem {

    private UUID id;

    private UUID cartId;

    private UUID menuItemId;

    private int quantity;

    private BigDecimal unitPriceSnapshot;

    private String note;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getCartId() {
        return cartId;
    }

    public void setCartId(UUID cartId) {
        this.cartId = cartId;
    }

    public UUID getMenuItemId() {
        return menuItemId;
    }

    public void setMenuItemId(UUID menuItemId) {
        this.menuItemId = menuItemId;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getUnitPriceSnapshot() {
        return unitPriceSnapshot;
    }

    public void setUnitPriceSnapshot(BigDecimal unitPriceSnapshot) {
        this.unitPriceSnapshot = unitPriceSnapshot;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    /**
     * Calculates the line total for this cart item.
     * Line total = unit price × quantity
     */
    public BigDecimal calculateLineTotal() {
        if (unitPriceSnapshot == null) {
            return BigDecimal.ZERO;
        }
        return unitPriceSnapshot.multiply(BigDecimal.valueOf(quantity));
    }

    /**
     * Updates the quantity with validation.
     *
     * @param newQuantity the new quantity (must be positive)
     * @throws IllegalArgumentException if quantity is not positive
     */
    public void updateQuantity(int newQuantity) {
        if (newQuantity <= 0) {
            throw new IllegalArgumentException("quantity must be positive");
        }
        this.quantity = newQuantity;
    }

    /**
     * Increases the quantity by the specified amount.
     *
     * @param additionalQuantity amount to add (must be positive)
     * @throws IllegalArgumentException if additionalQuantity is not positive
     */
    public void increaseQuantity(int additionalQuantity) {
        if (additionalQuantity <= 0) {
            throw new IllegalArgumentException("additionalQuantity must be positive");
        }
        this.quantity += additionalQuantity;
    }
}
