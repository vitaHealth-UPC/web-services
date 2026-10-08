package com.tata.familymonitoring.domain.model.entities;

import java.time.Instant;
import java.time.LocalDate;

/** A repeated-omission pattern found by Adherence Analytics, kept for the caregiver to read. */

public class AdherenceInsight {


  private Long id;

  private String medicationId;

  private String medicationName;

  private int omissionDays;

  private LocalDate firstDay;

  private LocalDate lastDay;

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

  /** Restores persisted state without replaying business actions. */
  public static AdherenceInsight rehydrate(Long id, String medicationId, String medicationName, int omissionDays, LocalDate firstDay, LocalDate lastDay, Instant detectedAt) {
    var restored = new AdherenceInsight();
    restored.id = id;
    restored.medicationId = medicationId;
    restored.medicationName = medicationName;
    restored.omissionDays = omissionDays;
    restored.firstDay = firstDay;
    restored.lastDay = lastDay;
    restored.detectedAt = detectedAt;
    return restored;
  }
}
