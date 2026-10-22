package com.vitahealth.tata.familymonitoring.domain.model.entities;

import java.time.Instant;
import java.util.Objects;

/** Note written by a caregiver about an intervention. */
public class CaregiverNote {

  public static final int MAX_LENGTH = 1000;

  private final Long id;

  private final String text;

  private final Instant recordedAt;

  private final String familiarId;

  /** Records a new intervention, removing surrounding whitespace from its text. */
  public CaregiverNote(String text, String familiarId, Instant recordedAt) {
    this(null, text, familiarId, recordedAt);
  }

  private CaregiverNote(Long id, String text, String familiarId, Instant recordedAt) {
    if (text == null || text.isBlank()) {
      throw new IllegalArgumentException("The note text must not be blank");
    }
    if (text.length() > MAX_LENGTH) {
      throw new IllegalArgumentException(
          "The note text must not exceed " + MAX_LENGTH + " characters");
    }
    if (familiarId == null || familiarId.isBlank()) {
      throw new IllegalArgumentException("The note author is required");
    }
    this.id = id;
    this.text = id == null ? text.strip() : text;
    this.familiarId = familiarId;
    this.recordedAt = Objects.requireNonNull(recordedAt, "recordedAt");
  }

  /** Returns the stored identity, absent until first persistence. */
  public Long getId() {
    return id;
  }

  /** Returns the intervention text as recorded. */
  public String getText() {
    return text;
  }

  /** Returns the original intervention recording instant. */
  public Instant getRecordedAt() {
    return recordedAt;
  }

  /** Returns the caregiver who recorded the intervention. */
  public String getFamiliarId() {
    return familiarId;
  }

  /** Restores validated stored evidence, preserving original text and timestamps. */
  public static CaregiverNote rehydrate(
      Long id, String text, Instant recordedAt, String familiarId) {
    if (id == null || id <= 0)
      throw new IllegalArgumentException("stored note identity must be positive");
    return new CaregiverNote(id, text, familiarId, recordedAt);
  }
}
