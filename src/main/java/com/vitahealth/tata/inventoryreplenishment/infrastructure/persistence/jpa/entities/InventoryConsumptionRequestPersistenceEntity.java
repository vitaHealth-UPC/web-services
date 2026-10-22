package com.vitahealth.tata.inventoryreplenishment.infrastructure.persistence.jpa.entities;

import com.vitahealth.tata.inventoryreplenishment.domain.model.valueobjects.PendingInventoryConsumption;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;

/** Stores original decrement intent separately from mutable retry metadata. */
@Entity
@Table(
    name = "inventory_consumption_requests",
    indexes =
        @Index(name = "ix_inventory_consumption_due", columnList = "next_attempt_at,intake_id"))
public class InventoryConsumptionRequestPersistenceEntity {
  @Id
  @Column(name = "intake_id", length = 36)
  private String intakeId;

  @Column(name = "medication_id", nullable = false, updatable = false, length = 36)
  private String medicationId;

  @Column(name = "older_adult_id", nullable = false, updatable = false, length = 36)
  private String olderAdultId;

  @Column(name = "confirmed_at", nullable = false, updatable = false)
  private Instant confirmedAt;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "next_attempt_at", nullable = false)
  private Instant nextAttemptAt;

  @Column(nullable = false)
  private int attempts;

  /** Required by JPA; production insertions use validated pending evidence. */
  protected InventoryConsumptionRequestPersistenceEntity() {}

  /** Copies immutable source evidence and its initial retry position. */
  public InventoryConsumptionRequestPersistenceEntity(PendingInventoryConsumption value) {
    intakeId = value.intakeId();
    medicationId = value.medicationId();
    olderAdultId = value.olderAdultId();
    confirmedAt = value.confirmedAt();
    createdAt = value.createdAt();
    nextAttemptAt = value.nextAttemptAt();
    attempts = value.attempts();
  }

  /** Restores a validated domain snapshot without returning a managed mutable entity. */
  public PendingInventoryConsumption toSnapshot() {
    return new PendingInventoryConsumption(
        intakeId, medicationId, olderAdultId, confirmedAt, createdAt, nextAttemptAt, attempts);
  }

  /** Changes retry position only; callers hold the request reservation. */
  public void retryAt(Instant next) {
    var updated = toSnapshot().retryAt(next);
    nextAttemptAt = updated.nextAttemptAt();
    attempts = updated.attempts();
  }
}
