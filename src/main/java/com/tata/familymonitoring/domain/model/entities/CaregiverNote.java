package com.tata.familymonitoring.domain.model.entities;

import java.time.Instant;

/** Note written by a caregiver about an intervention. */

public class CaregiverNote {

  public static final int MAX_LENGTH = 1000;


  private Long id;

  private String text;

  private Instant recordedAt;

  private String familiarId;

  protected CaregiverNote() {
  }

  public CaregiverNote(String text, String familiarId, Instant recordedAt) {
    if (text == null || text.isBlank()) {
      throw new IllegalArgumentException("The note text must not be blank");
    }
    if (text.length() > MAX_LENGTH) {
      throw new IllegalArgumentException("The note text must not exceed " + MAX_LENGTH + " characters");
    }
    if (familiarId == null || familiarId.isBlank()) {
      throw new IllegalArgumentException("The note author is required");
    }
    this.text = text.strip();
    this.familiarId = familiarId;
    this.recordedAt = recordedAt;
  }

  public Long getId() {
    return id;
  }

  public String getText() {
    return text;
  }

  public Instant getRecordedAt() {
    return recordedAt;
  }

  public String getFamiliarId() {
    return familiarId;
  }

  /** Restores persisted state without replaying business actions. */
  public static CaregiverNote rehydrate(Long id, String text, Instant recordedAt, String familiarId) {
    var restored = new CaregiverNote();
    restored.id = id;
    restored.text = text;
    restored.recordedAt = recordedAt;
    restored.familiarId = familiarId;
    return restored;
  }
}
