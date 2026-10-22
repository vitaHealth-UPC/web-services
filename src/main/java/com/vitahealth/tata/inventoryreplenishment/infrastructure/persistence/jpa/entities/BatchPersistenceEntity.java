package com.vitahealth.tata.inventoryreplenishment.infrastructure.persistence.jpa.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** Stores an immutable receipt fact owned by one inventory. */
@Entity
@Table(name = "inventory_batches")
public class BatchPersistenceEntity {
  @Id private String id;

  @Column(nullable = false)
  private int quantity;

  @Column(name = "registered_at", nullable = false)
  private Instant registeredAt;

  @Column(length = 200)
  private String lot;

  /** Returns the original optional lot label. */
  public String getLot() {
    return lot;
  }

  /** Required by JPA; receipt facts are created with complete source evidence. */
  protected BatchPersistenceEntity() {}

  /** Creates an unlabeled persisted receipt fact. */
  public BatchPersistenceEntity(String id, int quantity, Instant registeredAt) {
    this(id, quantity, registeredAt, null);
  }

  /** Copies receipt identity, units, original time and optional lot label. */
  public BatchPersistenceEntity(String id, int quantity, Instant registeredAt, String lot) {
    this.lot = lot;
    this.id = id;
    this.quantity = quantity;
    this.registeredAt = registeredAt;
  }

  /** Returns the receipt identity. */
  public String getId() {
    return id;
  }

  /** Returns received inventory units. */
  public int getQuantity() {
    return quantity;
  }

  /** Returns the original registration instant. */
  public Instant getRegisteredAt() {
    return registeredAt;
  }
}
