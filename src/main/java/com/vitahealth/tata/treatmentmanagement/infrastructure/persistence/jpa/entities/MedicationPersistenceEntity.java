package com.vitahealth.tata.treatmentmanagement.infrastructure.persistence.jpa.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** Stores medication evidence; business lifecycle rules remain in the domain aggregate. */
@Entity
@Table(name = "treatment_medications")
public class MedicationPersistenceEntity {
  @Id private String id;

  @Column(name = "older_adult_id", nullable = false, length = 36)
  private String olderAdultId;

  @Column(nullable = false, length = 160)
  private String name;

  @Column(nullable = false, length = 160)
  private String presentation;

  @Column(nullable = false)
  private boolean active;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  /** Required by JPA when restoring stored fields. */
  protected MedicationPersistenceEntity() {}

  /** Captures aggregate state without generating identity or registration timestamps. */
  public MedicationPersistenceEntity(
      String id,
      String olderAdultId,
      String name,
      String presentation,
      boolean active,
      Instant createdAt) {
    this.id = id;
    this.olderAdultId = olderAdultId;
    this.name = name;
    this.presentation = presentation;
    this.active = active;
    this.createdAt = createdAt;
  }

  /** Returns the original aggregate identity. */
  public String getId() {
    return id;
  }

  /** Returns the adult owning the stored aggregate. */
  public String getOlderAdultId() {
    return olderAdultId;
  }

  /** Returns the stored display name. */
  public String getName() {
    return name;
  }

  /** Returns the stored medication presentation. */
  public String getPresentation() {
    return presentation;
  }

  /** Returns whether the medication remains available for treatment activation. */
  public boolean isActive() {
    return active;
  }

  /** Returns the original registration instant. */
  public Instant getCreatedAt() {
    return createdAt;
  }
}
