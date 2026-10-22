package com.vitahealth.tata.inventoryreplenishment.infrastructure.persistence.jpa.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

/**
 * One row per confirmed intake already discounted; intake_id is a logical reference to Intake
 * Execution.
 */
@Entity
@Table(
    name = "inventory_consumptions",
    uniqueConstraints =
        @UniqueConstraint(name = "uk_inventory_consumptions_intake", columnNames = "intake_id"))
public class InventoryConsumptionPersistenceEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "intake_id", nullable = false, length = 36)
  private String intakeId;

  @Column(name = "inventory_id", nullable = false, length = 36)
  private String inventoryId;

  @Column(name = "consumed_at", nullable = false)
  private Instant consumedAt;

  /** Required by JPA field-access restoration. */
  protected InventoryConsumptionPersistenceEntity() {}

  /** Copies the confirmed intake marker persisted atomically with its stock decrement. */
  public InventoryConsumptionPersistenceEntity(
      String intakeId, String inventoryId, Instant consumedAt) {
    this.intakeId = intakeId;
    this.inventoryId = inventoryId;
    this.consumedAt = consumedAt;
  }

  /** Returns the stored marker identity. */
  public Long getId() {
    return id;
  }

  /** Returns the logical intake reference protected by the unique consumption constraint. */
  public String getIntakeId() {
    return intakeId;
  }

  /** Returns the inventory whose stock was decremented. */
  public String getInventoryId() {
    return inventoryId;
  }

  /** Returns the application clock instant recorded with the decrement. */
  public Instant getConsumedAt() {
    return consumedAt;
  }
}
