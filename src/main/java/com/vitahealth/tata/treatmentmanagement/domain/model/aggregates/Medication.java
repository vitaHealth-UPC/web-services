package com.vitahealth.tata.treatmentmanagement.domain.model.aggregates;

import static com.vitahealth.tata.shared.domain.validation.DomainText.requireText;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Owns medication identity, descriptive data and availability independently of its treatments. */
public final class Medication {
  private static final int MAX_DESCRIPTION_LENGTH = 160;
  private final String id;
  private final String olderAdultId;
  private String name;
  private String presentation;
  private boolean active;
  private final Instant createdAt;

  private Medication(
      String id,
      String olderAdultId,
      String name,
      String presentation,
      boolean active,
      Instant createdAt) {
    this.id = requireText(id, "id");
    this.olderAdultId = requireText(olderAdultId, "olderAdultId");
    this.name = description(name, "name");
    this.presentation = description(presentation, "presentation");
    this.active = active;
    this.createdAt = Objects.requireNonNull(createdAt);
  }

  /** Registers active medication evidence with the supplied occurrence time. */
  public static Medication register(
      String olderAdultId, String name, String presentation, Instant now) {
    return new Medication(
        UUID.randomUUID().toString(), olderAdultId, name, presentation, true, now);
  }

  /** Restores persisted medication without changing identity, availability or creation time. */
  public static Medication rehydrate(
      String id,
      String olderAdultId,
      String name,
      String presentation,
      boolean active,
      Instant createdAt) {
    return new Medication(id, olderAdultId, name, presentation, active, createdAt);
  }

  /** Validates both replacement fields before mutating an active medication. */
  public void update(String name, String presentation) {
    if (!active) throw new IllegalStateException("inactive medication cannot be edited");
    var validatedName = description(name, "name");
    var validatedPresentation = description(presentation, "presentation");
    this.name = validatedName;
    this.presentation = validatedPresentation;
  }

  /** Makes the medication unavailable while retaining its identity and description. */
  public void deactivate() {
    active = false;
  }

  /** Restores availability without changing identity or restarting any dependent regimen. */
  public void reactivate() {
    active = true;
  }

  private static String description(String value, String field) {
    var normalized = requireText(value, field);
    if (normalized.length() > MAX_DESCRIPTION_LENGTH) {
      throw new IllegalArgumentException(field + " exceeds the medication description limit");
    }
    return normalized;
  }

  /** Returns the stable medication identity. */
  public String id() {
    return id;
  }

  /** Returns the adult profile owning this medication. */
  public String olderAdultId() {
    return olderAdultId;
  }

  /** Returns the validated medication name. */
  public String name() {
    return name;
  }

  /** Returns the validated presentation description. */
  public String presentation() {
    return presentation;
  }

  /** Reports whether new treatment configuration may use this medication. */
  public boolean active() {
    return active;
  }

  /** Returns original registration time without consulting the current clock. */
  public Instant createdAt() {
    return createdAt;
  }
}
