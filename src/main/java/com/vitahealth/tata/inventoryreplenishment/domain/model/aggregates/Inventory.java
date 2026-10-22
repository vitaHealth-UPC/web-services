package com.vitahealth.tata.inventoryreplenishment.domain.model.aggregates;

import static com.vitahealth.tata.shared.domain.validation.DomainText.requireText;

import com.vitahealth.tata.inventoryreplenishment.domain.model.entities.Batch;
import com.vitahealth.tata.inventoryreplenishment.domain.model.events.LowStockDetected;
import com.vitahealth.tata.inventoryreplenishment.domain.model.events.ReplenishmentRegistered;
import com.vitahealth.tata.inventoryreplenishment.domain.model.valueobjects.StockLevel;
import com.vitahealth.tata.shared.domain.model.aggregates.AbstractDomainAggregateRoot;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Stock of one medication. medicationId is a logical reference to Treatment Management; there is no
 * physical foreign key across Bounded Contexts.
 */
public final class Inventory extends AbstractDomainAggregateRoot {
  private final String id;
  private Long revision;
  private final String medicationId;
  private int remainingStock;
  private final int replenishmentThreshold;
  private final List<Batch> batches;
  private final Instant createdAt;
  private Instant updatedAt;

  private Inventory(
      String id,
      String medicationId,
      int remainingStock,
      int replenishmentThreshold,
      List<Batch> batches,
      Instant createdAt,
      Instant updatedAt) {
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
      String medicationId, int initialQuantity, int replenishmentThreshold, Instant now) {
    Objects.requireNonNull(now);
    var inventory =
        new Inventory(
            UUID.randomUUID().toString(),
            medicationId,
            0,
            replenishmentThreshold,
            List.of(),
            now,
            now);
    inventory.addBatch(Batch.register(initialQuantity, now), now);
    return inventory;
  }

  /** Restores validated receipt history without publishing historical domain events. */
  public static Inventory rehydrate(
      String id,
      String medicationId,
      int remainingStock,
      int replenishmentThreshold,
      List<Batch> batches,
      Instant createdAt,
      Instant updatedAt,
      Long revision) {
    if (revision == null || revision < 0)
      throw new IllegalArgumentException("stored inventory revision must be nonnegative");
    var receipts = List.copyOf(Objects.requireNonNull(batches, "batches"));
    Objects.requireNonNull(createdAt, "createdAt");
    Objects.requireNonNull(updatedAt, "updatedAt");
    if (updatedAt.isBefore(createdAt))
      throw new IllegalArgumentException("inventory update must not precede creation");
    if (receipts.isEmpty())
      throw new IllegalArgumentException("stored inventory must include its initial receipt");
    var identities = new HashSet<String>();
    long receivedUnits = 0;
    for (var batch : receipts) {
      if (!identities.add(batch.id()))
        throw new IllegalArgumentException("stored inventory contains duplicate receipts");
      if (batch.registeredAt().isBefore(createdAt) || batch.registeredAt().isAfter(updatedAt))
        throw new IllegalArgumentException("receipt time must fit the inventory audit interval");
      receivedUnits = Math.addExact(receivedUnits, batch.quantity());
    }
    if (remainingStock > receivedUnits)
      throw new IllegalArgumentException("remaining stock must not exceed received units");
    var restored =
        new Inventory(
            id,
            medicationId,
            remainingStock,
            replenishmentThreshold,
            receipts,
            createdAt,
            updatedAt);
    restored.revision = revision;
    return restored;
  }

  /** US-43: a replenishment adds a batch and increases the remaining stock. */
  public Batch registerBatch(int quantity, Instant now) {
    return registerBatch(quantity, now, null);
  }

  /** Adds a labeled receipt after validating its time and quantity, then records replenishment. */
  public Batch registerBatch(int quantity, Instant now, String lot) {
    requireCurrentTime(now);
    var batch = Batch.register(quantity, now, lot);
    addBatch(batch, now);
    registerDomainEvent(
        new ReplenishmentRegistered(
            id, medicationId, batch.id(), batch.quantity(), remainingStock, now));
    return batch;
  }

  /**
   * Consumes one unit. Stock can never become negative. LowStockDetected is recorded only when this
   * consumption crosses the threshold, not on every consumption below it.
   */
  public void consumeUnit(Instant now) {
    requireCurrentTime(now);
    if (remainingStock == 0) {
      throw new IllegalStateException("stock cannot be negative");
    }
    var wasLow = isLowStock();
    remainingStock--;
    updatedAt = now;
    if (!wasLow && isLowStock()) {
      registerDomainEvent(
          new LowStockDetected(id, medicationId, remainingStock, replenishmentThreshold, now));
    }
  }

  /** Reports whether remaining stock is at or below the replenishment threshold. */
  public boolean isLowStock() {
    return stockLevel().isLow();
  }

  /** Returns the stock and threshold expressed in inventory units. */
  public StockLevel stockLevel() {
    return new StockLevel(remainingStock, replenishmentThreshold);
  }

  private void requireCurrentTime(Instant now) {
    Objects.requireNonNull(now, "now");
    if (now.isBefore(updatedAt))
      throw new IllegalArgumentException("inventory change must not precede its last update");
  }

  private void addBatch(Batch batch, Instant now) {
    remainingStock = Math.addExact(remainingStock, batch.quantity());
    batches.add(batch);
    updatedAt = Objects.requireNonNull(now);
  }

  /** Returns the observed persistence revision, absent until initial storage. */
  public Long revision() {
    return revision;
  }

  /** Returns the inventory identity. */
  public String id() {
    return id;
  }

  /** Returns the logical Treatment medication reference. */
  public String medicationId() {
    return medicationId;
  }

  /** Returns the remaining inventory units. */
  public int remainingStock() {
    return remainingStock;
  }

  /** Returns the inclusive low-stock threshold. */
  public int replenishmentThreshold() {
    return replenishmentThreshold;
  }

  /** Returns an immutable copy of the complete receipt history. */
  public List<Batch> batches() {
    return List.copyOf(batches);
  }

  /** Returns the initial stock registration instant. */
  public Instant createdAt() {
    return createdAt;
  }

  /** Returns the latest applied stock mutation instant. */
  public Instant updatedAt() {
    return updatedAt;
  }
}
