package com.vitahealth.tata.familymonitoring.infrastructure.persistence.jpa.entities;

import com.vitahealth.tata.familymonitoring.domain.model.valueobjects.FamilyStockCatchUp;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** Persists the recoverable medication cursor belonging to a stored family monitor. */
@Entity
@Table(name = "family_stock_catchups")
public class FamilyStockCatchUpPersistenceEntity {
  @Id
  @Column(name = "family_monitor_id")
  private Long monitorId;

  @Column(nullable = false, length = 36)
  private String afterMedicationId;

  @Column(nullable = false, updatable = false)
  private Instant requestedAt;

  @Column(nullable = false)
  private Instant nextAttemptAt;

  @Column(nullable = false)
  private int attempts;

  /** Required by JPA field-access restoration. */
  protected FamilyStockCatchUpPersistenceEntity() {}

  /** Copies validated new work while preserving its original request evidence. */
  public FamilyStockCatchUpPersistenceEntity(FamilyStockCatchUp request) {
    monitorId = request.monitorId();
    requestedAt = request.requestedAt();
    apply(request);
  }

  /** Restores and validates a detached immutable request snapshot. */
  public FamilyStockCatchUp toSnapshot() {
    return new FamilyStockCatchUp(
        monitorId, afterMedicationId, requestedAt, nextAttemptAt, attempts);
  }

  /** Replaces mutable processing evidence only for this exact original request. */
  public void apply(FamilyStockCatchUp request) {
    if (monitorId != request.monitorId() || !requestedAt.equals(request.requestedAt()))
      throw new IllegalStateException("catch-up request identity cannot change");
    if (afterMedicationId != null && request.afterMedicationId().compareTo(afterMedicationId) < 0)
      throw new IllegalStateException("catch-up progress cannot move backwards");
    afterMedicationId = request.afterMedicationId();
    nextAttemptAt = request.nextAttemptAt();
    attempts = request.attempts();
  }
}
