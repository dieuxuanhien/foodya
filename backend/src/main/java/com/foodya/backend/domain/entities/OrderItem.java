package com.foodya.backend.domain.entities;

import java.math.BigDecimal;
import java.util.UUID;

public class OrderItem {

    private UUID id;

    private UUID orderId;

    private UUID menuItemId;

    private String menuItemNameSnapshot;

    private BigDecimal unitPriceSnapshot;

    private int quantity;

    private BigDecimal lineTotal;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public void setOrderId(UUID orderId) {
        this.orderId = orderId;
    }

    public UUID getMenuItemId() {
        return menuItemId;
    }

    public void setMenuItemId(UUID menuItemId) {
        this.menuItemId = menuItemId;
    }

    public String getMenuItemNameSnapshot() {
        return menuItemNameSnapshot;
    }

    public void setMenuItemNameSnapshot(String menuItemNameSnapshot) {
        this.menuItemNameSnapshot = menuItemNameSnapshot;
    }

    public BigDecimal getUnitPriceSnapshot() {
        return unitPriceSnapshot;
    }

    public void setUnitPriceSnapshot(BigDecimal unitPriceSnapshot) {
        this.unitPriceSnapshot = unitPriceSnapshot;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getLineTotal() {
        return lineTotal;
    }

    public void setLineTotal(BigDecimal lineTotal) {
        this.lineTotal = lineTotal;
    }
}
