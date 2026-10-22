package com.vitahealth.tata.inventoryreplenishment.infrastructure.persistence.jpa.entities;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

/**
 * medication_id is a logical reference to Treatment Management: unique, but without a foreign key.
 * The version column rejects concurrent updates of the same inventory (optimistic locking).
 */
@Entity
@Table(
    name = "inventories",
    uniqueConstraints =
        @UniqueConstraint(name = "uk_inventories_medication", columnNames = "medication_id"))
public class InventoryPersistenceEntity {
  @Id private String id;

  @Column(name = "medication_id", nullable = false, length = 36)
  private String medicationId;

  @Column(name = "remaining_stock", nullable = false)
  private int remainingStock;

  @Column(name = "replenishment_threshold", nullable = false)
  private int replenishmentThreshold;

  @OneToMany(
      cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REMOVE},
      orphanRemoval = true)
  @JoinColumn(name = "inventory_id", nullable = false)
  @OrderBy("registeredAt ASC")
  private List<BatchPersistenceEntity> batches = new ArrayList<>();

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Version private Long version;

  /** Required by JPA; application code creates complete parent state explicitly. */
  protected InventoryPersistenceEntity() {}

  /** Starts managed stock for a logical medication reference and original audit instants. */
  public InventoryPersistenceEntity(
      String id,
      String medicationId,
      int remainingStock,
      int replenishmentThreshold,
      Instant createdAt,
      Instant updatedAt) {
    this.id = id;
    this.medicationId = medicationId;
    this.remainingStock = remainingStock;
    this.replenishmentThreshold = replenishmentThreshold;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  /** Validates complete receipt history before changing stock or appending new managed facts. */
  public void updateStock(
      int remainingStock, List<BatchPersistenceEntity> currentBatches, Instant updatedAt) {
    Objects.requireNonNull(updatedAt, "updatedAt");
    if (updatedAt.isBefore(this.updatedAt) || remainingStock < 0)
      throw new IllegalStateException("invalid inventory stock transition");
    var incoming = new HashMap<String, BatchPersistenceEntity>();
    for (var receipt : List.copyOf(currentBatches)) {
      if (receipt.getId() == null
          || receipt.getId().isBlank()
          || receipt.getQuantity() <= 0
          || receipt.getRegisteredAt() == null
          || receipt.getRegisteredAt().isBefore(createdAt)
          || receipt.getRegisteredAt().isAfter(updatedAt)
          || incoming.putIfAbsent(receipt.getId(), receipt) != null)
        throw new IllegalStateException("invalid inventory receipt history");
    }
    for (var stored : batches) {
      var replacement = incoming.remove(stored.getId());
      if (replacement == null
          || stored.getQuantity() != replacement.getQuantity()
          || !stored.getRegisteredAt().equals(replacement.getRegisteredAt())
          || !Objects.equals(stored.getLot(), replacement.getLot()))
        throw new IllegalStateException("stored inventory receipts cannot change or disappear");
    }
    long available = this.remainingStock;
    for (var added : incoming.values()) {
      if (added.getRegisteredAt().isBefore(this.updatedAt))
        throw new IllegalStateException("new inventory receipt predates current stock");
      available = Math.addExact(available, added.getQuantity());
    }
    if (incoming.isEmpty() && batches.isEmpty())
      throw new IllegalStateException("inventory requires an initial receipt");
    if (remainingStock > available)
      throw new IllegalStateException("inventory stock increase requires receipt evidence");
    // Preserve source order and managed instances after the complete candidate is validated.
    for (var receipt : currentBatches) {
      if (incoming.containsKey(receipt.getId())) batches.add(receipt);
    }
    this.remainingStock = remainingStock;
    this.updatedAt = updatedAt;
  }

  /** Returns the revision used to reject obsolete domain snapshots. */
  public Long getVersion() {
    return version;
  }

  /** Returns the stored inventory identity. */
  public String getId() {
    return id;
  }

  /** Returns the unique logical medication reference. */
  public String getMedicationId() {
    return medicationId;
  }

  /** Returns current inventory units. */
  public int getRemainingStock() {
    return remainingStock;
  }

  /** Returns the inclusive low-stock threshold. */
  public int getReplenishmentThreshold() {
    return replenishmentThreshold;
  }

  /** Returns managed receipt history for transaction-local persistence mapping. */
  public List<BatchPersistenceEntity> getBatches() {
    return Collections.unmodifiableList(batches);
  }

  /** Returns the original registration instant. */
  public Instant getCreatedAt() {
    return createdAt;
  }

  /** Returns the latest stored mutation instant. */
  public Instant getUpdatedAt() {
    return updatedAt;
  }
}
