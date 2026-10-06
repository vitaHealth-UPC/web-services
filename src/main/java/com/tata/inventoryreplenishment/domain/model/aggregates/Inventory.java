package com.tata.inventoryreplenishment.domain.model.aggregates;

import com.tata.inventoryreplenishment.domain.model.entities.Batch;
import com.tata.inventoryreplenishment.domain.model.events.LowStockDetected;
import com.tata.inventoryreplenishment.domain.model.events.ReplenishmentRegistered;
import com.tata.inventoryreplenishment.domain.model.valueobjects.StockLevel;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Stock of one medication. medicationId is a logical reference to Treatment Management;
 * there is no physical foreign key across Bounded Contexts.
 */
public final class Inventory {
    private final String id;
    private final String medicationId;
    private int remainingStock;
    private final int replenishmentThreshold;
    private final List<Batch> batches;
    private final Instant createdAt;
    private Instant updatedAt;
    private final List<Object> domainEvents = new ArrayList<>();

    private Inventory(
            String id,
            String medicationId,
            int remainingStock,
            int replenishmentThreshold,
            List<Batch> batches,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = requireText(id, "id");
        this.medicationId = requireText(medicationId, "medicationId");
        new StockLevel(remainingStock, replenishmentThreshold);
        this.remainingStock = remainingStock;
        this.replenishmentThreshold = replenishmentThreshold;
        this.batches = new ArrayList<>(Objects.requireNonNull(batches));
        this.createdAt = Objects.requireNonNull(createdAt);
        this.updatedAt = Objects.requireNonNull(updatedAt);
    }

    /** US-40: the initial stock is registered as the first batch. */
    public static Inventory registerInitial(
            String medicationId,
            int initialQuantity,
            int replenishmentThreshold,
            Instant now
    ) {
        Objects.requireNonNull(now);
        var inventory = new Inventory(
                UUID.randomUUID().toString(),
                medicationId,
                0,
                replenishmentThreshold,
                List.of(),
                now,
                now
        );
        inventory.addBatch(Batch.register(initialQuantity, now), now);
        return inventory;
    }

    public static Inventory rehydrate(
            String id,
            String medicationId,
            int remainingStock,
            int replenishmentThreshold,
            List<Batch> batches,
            Instant createdAt,
            Instant updatedAt
    ) {
        return new Inventory(id, medicationId, remainingStock, replenishmentThreshold, batches, createdAt, updatedAt);
    }

    /** US-43: a replenishment adds a batch and increases the remaining stock. */
    public Batch registerBatch(int quantity, Instant now) {
        var batch = Batch.register(quantity, now);
        addBatch(batch, now);
        domainEvents.add(new ReplenishmentRegistered(id, medicationId, batch.id(), batch.quantity(), remainingStock, now));
        return batch;
    }

    /**
     * Consumes one unit. Stock can never become negative. LowStockDetected is recorded only
     * when this consumption crosses the threshold, not on every consumption below it.
     */
    public void consumeUnit(Instant now) {
        Objects.requireNonNull(now);
        if (remainingStock == 0) {
            throw new IllegalStateException("stock cannot be negative");
        }
        var wasLow = isLowStock();
        remainingStock--;
        updatedAt = now;
        if (!wasLow && isLowStock()) {
            domainEvents.add(new LowStockDetected(id, medicationId, remainingStock, replenishmentThreshold, now));
        }
    }

    public boolean isLowStock() {
        return stockLevel().isLow();
    }

    public StockLevel stockLevel() {
        return new StockLevel(remainingStock, replenishmentThreshold);
    }

    /** Returns the events recorded since the last call and clears them. */
    public List<Object> pullDomainEvents() {
        var events = List.copyOf(domainEvents);
        domainEvents.clear();
        return events;
    }

    private void addBatch(Batch batch, Instant now) {
        remainingStock = Math.addExact(remainingStock, batch.quantity());
        batches.add(batch);
        updatedAt = Objects.requireNonNull(now);
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    public String id() { return id; }
    public String medicationId() { return medicationId; }
    public int remainingStock() { return remainingStock; }
    public int replenishmentThreshold() { return replenishmentThreshold; }
    public List<Batch> batches() { return List.copyOf(batches); }
    public Instant createdAt() { return createdAt; }
    public Instant updatedAt() { return updatedAt; }
}
