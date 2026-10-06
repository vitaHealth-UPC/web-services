package com.tata.familymonitoring.domain.model.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.Instant;

/** Note written by a caregiver about an intervention. */
@Entity
public class CaregiverNote {

  public static final int MAX_LENGTH = 1000;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = MAX_LENGTH)
  private String text;

  @Column(nullable = false)
  private Instant recordedAt;

  @Column(nullable = false, length = 36)
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
}
