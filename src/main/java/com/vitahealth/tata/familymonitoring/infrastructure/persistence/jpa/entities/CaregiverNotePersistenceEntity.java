package com.vitahealth.tata.familymonitoring.infrastructure.persistence.jpa.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** Stores immutable caregiver intervention evidence owned by one relationship follow-up. */
@Entity
@Table(name = "caregiver_notes")
public class CaregiverNotePersistenceEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 1000)
  private String text;

  @Column(nullable = false)
  private Instant recordedAt;

  @Column(nullable = false, length = 36)
  private String familiarId;

  /** Required by the persistence provider. */
  public CaregiverNotePersistenceEntity() {}

  /** Returns stored row identity. */
  public Long getId() {
    return id;
  }

  /** Copies stored row identity during managed persistence assembly or update. */
  public void setId(Long value) {
    this.id = value;
  }

  /** Returns recorded intervention content. */
  public String getText() {
    return text;
  }

  /** Copies recorded intervention content during managed persistence assembly or update. */
  public void setText(String value) {
    this.text = value;
  }

  /** Returns original intervention recording instant. */
  public Instant getRecordedAt() {
    return recordedAt;
  }

  /**
   * Copies original intervention recording instant during managed persistence assembly or update.
   */
  public void setRecordedAt(Instant value) {
    this.recordedAt = value;
  }

  /** Returns caregiver author or relationship owner reference. */
  public String getFamiliarId() {
    return familiarId;
  }

  /**
   * Copies caregiver author or relationship owner reference during managed persistence assembly or
   * update.
   */
  public void setFamiliarId(String value) {
    this.familiarId = value;
  }
}
