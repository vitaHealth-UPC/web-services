package com.vitahealth.tata.inventoryreplenishment.domain.model.entities;

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
    if (lot != null && lot.length() > 200)
      throw new IllegalArgumentException("lot must not exceed 200 characters");
    this.lot = lot;
    this.id = id.trim();
    this.quantity = quantity;
    this.registeredAt = Objects.requireNonNull(registeredAt);
  }

  /** Registers an unlabeled positive receipt with a new identity. */
  public static Batch register(int quantity, Instant now) {
    return register(quantity, now, null);
  }

  /** Restores an unlabeled receipt with its original identity and instant. */
  public static Batch rehydrate(String id, int quantity, Instant registeredAt) {
    return rehydrate(id, quantity, registeredAt, null);
  }

  /** Registers a receipt, normalizing an optional user-entered lot label. */
  public static Batch register(int quantity, Instant now, String lot) {
    var normalizedLot = lot == null || lot.isBlank() ? null : lot.strip();
    return new Batch(UUID.randomUUID().toString(), quantity, now, normalizedLot);
  }

  /** Restores validated receipt evidence while preserving stored lot text. */
  public static Batch rehydrate(String id, int quantity, Instant registeredAt, String lot) {
    return new Batch(id, quantity, registeredAt, lot);
  }

  /** Returns the optional lot label recorded with this receipt. */
  public String lot() {
    return lot;
  }

  /** Returns the immutable receipt identity. */
  public String id() {
    return id;
  }

  /** Returns the positive received quantity in inventory units. */
  public int quantity() {
    return quantity;
  }

  /** Returns the original receipt instant. */
  public Instant registeredAt() {
    return registeredAt;
  }
}
