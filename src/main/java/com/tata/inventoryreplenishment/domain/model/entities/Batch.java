package com.tata.inventoryreplenishment.domain.model.entities;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** A quantity of units added to an inventory at a given moment. */
public final class Batch {
    private final String id;
    private final int quantity;
    private final Instant registeredAt;
    private final String lot;

    private Batch(String id, int quantity, Instant registeredAt, String lot) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("batch id is required");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("batch quantity must be positive");
        }
        lot = lot == null || lot.isBlank() ? null : lot.trim();
        if (lot != null && lot.length() > 200) throw new IllegalArgumentException("lot must not exceed 200 characters");
        this.lot = lot;
        this.id = id.trim();
        this.quantity = quantity;
        this.registeredAt = Objects.requireNonNull(registeredAt);
    }

    public static Batch register(int quantity, Instant now) {
        return register(quantity, now, null);
    }

    public static Batch rehydrate(String id, int quantity, Instant registeredAt) {
        return rehydrate(id, quantity, registeredAt, null);
    }

    public static Batch register(int quantity, Instant now, String lot) { return new Batch(UUID.randomUUID().toString(), quantity, now, lot); }
    public static Batch rehydrate(String id, int quantity, Instant registeredAt, String lot) { return new Batch(id, quantity, registeredAt, lot); }
    public String lot() { return lot; }
    public String id() { return id; }
    public int quantity() { return quantity; }
    public Instant registeredAt() { return registeredAt; }
}
