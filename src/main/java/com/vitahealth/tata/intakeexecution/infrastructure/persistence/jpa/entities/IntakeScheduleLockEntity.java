package com.vitahealth.tata.intakeexecution.infrastructure.persistence.jpa.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;

/** Stores a durable coordination key without owning clinical treatment or intake history. */
@Entity
@Table(
    name = "intake_schedule_locks",
    uniqueConstraints =
        @UniqueConstraint(name = "uk_intake_schedule_lock_treatment", columnNames = "treatment_id"))
public class IntakeScheduleLockEntity {
  @Id
  @Column(nullable = false, updatable = false, length = 36)
  private String id;

  @Column(name = "treatment_id", nullable = false, updatable = false, length = 36)
  private String treatmentId;

  /** Required by JPA. */
  protected IntakeScheduleLockEntity() {}

  /** Creates a technical reservation key independently of future intake row existence. */
  public IntakeScheduleLockEntity(String treatmentId) {
    this.id = UUID.randomUUID().toString();
    this.treatmentId = treatmentId;
  }
}
