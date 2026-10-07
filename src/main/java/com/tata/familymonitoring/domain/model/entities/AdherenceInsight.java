package com.tata.familymonitoring.domain.model.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.Instant;
import java.time.LocalDate;

/** A repeated-omission pattern found by Adherence Analytics, kept for the caregiver to read. */
@Entity
public class AdherenceInsight {

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

  protected AdherenceInsight() {
  }

  public AdherenceInsight(
      String medicationId, String medicationName, int omissionDays, LocalDate firstDay, LocalDate lastDay,
      Instant detectedAt) {
    this.medicationId = medicationId;
    this.medicationName = medicationName;
    this.omissionDays = omissionDays;
    this.firstDay = firstDay;
    this.lastDay = lastDay;
    this.detectedAt = detectedAt;
  }

  public boolean isSamePattern(String medicationId, LocalDate firstDay, LocalDate lastDay) {
    return this.medicationId.equals(medicationId) && this.firstDay.equals(firstDay) && this.lastDay.equals(lastDay);
  }

  public Long getId() {
    return id;
  }

  public String getMedicationId() {
    return medicationId;
  }

  public String getMedicationName() {
    return medicationName;
  }

  public int getOmissionDays() {
    return omissionDays;
  }

  public LocalDate getFirstDay() {
    return firstDay;
  }

  public LocalDate getLastDay() {
    return lastDay;
  }

  public Instant getDetectedAt() {
    return detectedAt;
  }
}
