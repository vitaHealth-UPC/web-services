package com.vitahealth.tata.familymonitoring.infrastructure.persistence.jpa.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.time.LocalDate;

/** Stores original source analytics patterns owned by one relationship follow-up. */
@Entity
@Table(
    name = "adherence_insights",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uq_follow_up_adherence_pattern",
            columnNames = {"family_monitor_id", "medication_id", "first_day", "last_day"}))
public class AdherenceInsightPersistenceEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 36)
  private String medicationId;

  @Column(nullable = false)
  private String medicationName;

  @Column(nullable = false)
  private int omissionDays;

  @Column(nullable = false)
  private LocalDate firstDay;

  @Column(nullable = false)
  private LocalDate lastDay;

  @Column(nullable = false)
  private Instant detectedAt;

  /** Required by the persistence provider. */
  public AdherenceInsightPersistenceEntity() {}

  /** Returns stored row identity. */
  public Long getId() {
    return id;
  }

  /** Copies stored row identity during managed persistence assembly or update. */
  public void setId(Long value) {
    this.id = value;
  }

  /** Returns source medication reference. */
  public String getMedicationId() {
    return medicationId;
  }

  /** Copies source medication reference during managed persistence assembly or update. */
  public void setMedicationId(String value) {
    this.medicationId = value;
  }

  /** Returns medication name recorded in source evidence. */
  public String getMedicationName() {
    return medicationName;
  }

  /**
   * Copies medication name recorded in source evidence during managed persistence assembly or
   * update.
   */
  public void setMedicationName(String value) {
    this.medicationName = value;
  }

  /** Returns source omission-day count. */
  public int getOmissionDays() {
    return omissionDays;
  }

  /** Copies source omission-day count during managed persistence assembly or update. */
  public void setOmissionDays(int value) {
    this.omissionDays = value;
  }

  /** Returns first inclusive source pattern date. */
  public LocalDate getFirstDay() {
    return firstDay;
  }

  /** Copies first inclusive source pattern date during managed persistence assembly or update. */
  public void setFirstDay(LocalDate value) {
    this.firstDay = value;
  }

  /** Returns last inclusive source pattern date. */
  public LocalDate getLastDay() {
    return lastDay;
  }

  /** Copies last inclusive source pattern date during managed persistence assembly or update. */
  public void setLastDay(LocalDate value) {
    this.lastDay = value;
  }

  /** Returns original source detection instant. */
  public Instant getDetectedAt() {
    return detectedAt;
  }

  /** Copies original source detection instant during managed persistence assembly or update. */
  public void setDetectedAt(Instant value) {
    this.detectedAt = value;
  }
}
