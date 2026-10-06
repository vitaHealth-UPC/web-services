package com.tata.inventoryreplenishment.domain.model.entities;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** A quantity of units added to an inventory at a given moment. */
public final class Batch {
    private final String id;
    private final int quantity;
    private final Instant registeredAt;

    private Batch(String id, int quantity, Instant registeredAt) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("batch id is required");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("batch quantity must be positive");
        }
        this.id = id.trim();
        this.quantity = quantity;
        this.registeredAt = Objects.requireNonNull(registeredAt);
    }

    public static Batch register(int quantity, Instant now) {
        return new Batch(UUID.randomUUID().toString(), quantity, now);
    }

    public static Batch rehydrate(String id, int quantity, Instant registeredAt) {
        return new Batch(id, quantity, registeredAt);
    }

    public String id() { return id; }
    public int quantity() { return quantity; }
    public Instant registeredAt() { return registeredAt; }
}
