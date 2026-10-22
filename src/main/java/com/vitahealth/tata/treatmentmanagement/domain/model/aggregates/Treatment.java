package com.vitahealth.tata.treatmentmanagement.domain.model.aggregates;

import static com.vitahealth.tata.shared.domain.validation.DomainText.requireText;

import com.vitahealth.tata.treatmentmanagement.domain.model.valueobjects.TreatmentRegimen;
import com.vitahealth.tata.treatmentmanagement.domain.model.valueobjects.TreatmentStatus;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Owns the treatment lifecycle and its optional draft regimen independently of medication data. */
public final class Treatment {
  private final String id;
  private final String olderAdultId;
  private final String name;
  private TreatmentStatus status;
  private TreatmentRegimen regimen;
  private final Instant createdAt;

  private Treatment(
      String id,
      String olderAdultId,
      String name,
      TreatmentStatus status,
      TreatmentRegimen regimen,
      Instant createdAt) {
    this.id = requireText(id, "id");
    this.olderAdultId = requireText(olderAdultId, "olderAdultId");
    this.name = requireText(name, "name");
    if (this.name.length() > 160)
      throw new IllegalArgumentException("treatment name exceeds its storage bound");
    this.status = Objects.requireNonNull(status);
    if (status != TreatmentStatus.DRAFT && regimen == null) {
      throw new IllegalArgumentException("non-draft treatment requires a regimen");
    }
    this.regimen = regimen;
    this.createdAt = Objects.requireNonNull(createdAt);
  }

  /** Creates an unconfigured draft using the supplied registration time. */
  public static Treatment create(String olderAdultId, String name, Instant now) {
    return new Treatment(
        UUID.randomUUID().toString(), olderAdultId, name, TreatmentStatus.DRAFT, null, now);
  }

  /** Restores identity and lifecycle evidence without synthesizing a missing regimen. */
  public static Treatment rehydrate(
      String id,
      String olderAdultId,
      String name,
      TreatmentStatus status,
      TreatmentRegimen regimen,
      Instant createdAt) {
    return new Treatment(id, olderAdultId, name, status, regimen, createdAt);
  }

  /** Replaces the regimen only after a complete value has been constructed. */
  public void configure(TreatmentRegimen regimen) {
    this.regimen = Objects.requireNonNull(regimen);
  }

  /** Activates configured evidence, allowing an already active request to be repeated. */
  public void activate() {
    if (regimen == null) throw new IllegalStateException("treatment regimen is incomplete");
    status = TreatmentStatus.ACTIVE;
  }

  /** Pauses an active treatment while preserving its configured regimen. */
  public void pause() {
    if (status != TreatmentStatus.ACTIVE)
      throw new IllegalStateException("only an active treatment can be paused");
    status = TreatmentStatus.PAUSED;
  }

  /** Resumes a paused treatment with its existing configured regimen. */
  public void resume() {
    if (status != TreatmentStatus.PAUSED)
      throw new IllegalStateException("only a paused treatment can be resumed");
    if (regimen == null) throw new IllegalStateException("treatment regimen is incomplete");
    status = TreatmentStatus.ACTIVE;
  }

  /** Returns the stable treatment identity. */
  public String id() {
    return id;
  }

  /** Returns the adult owning this treatment. */
  public String olderAdultId() {
    return olderAdultId;
  }

  /** Returns its validated display name. */
  public String name() {
    return name;
  }

  /** Returns its current lifecycle state. */
  public TreatmentStatus status() {
    return status;
  }

  /** Returns the immutable regimen or null for an unconfigured draft. */
  public TreatmentRegimen regimen() {
    return regimen;
  }

  /** Returns original creation time without reading the current clock. */
  public Instant createdAt() {
    return createdAt;
  }
}
