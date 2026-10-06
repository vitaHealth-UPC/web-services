package com.tata.inventoryreplenishment.domain.model.valueobjects;

/**
 * Remaining units of a medication compared with its replenishment threshold.
 * Stock is considered low when it reaches or falls below the threshold.
 */
public record StockLevel(int remaining, int threshold) {
    public StockLevel {
        if (remaining < 0) {
            throw new IllegalArgumentException("remaining stock cannot be negative");
        }
        if (threshold < 0) {
            throw new IllegalArgumentException("replenishment threshold cannot be negative");
        }
    }

    public boolean isLow() {
        return remaining <= threshold;
    }
}
