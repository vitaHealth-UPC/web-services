package com.vitahealth.tata.familymonitoring.domain.model.entities;

import static com.vitahealth.tata.shared.domain.validation.DomainText.requireText;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/** A repeated-omission pattern found by Adherence Analytics, kept for the caregiver to read. */
public class AdherenceInsight {

  private Long id;

  private final String medicationId;

  private final String medicationName;

  private final int omissionDays;

  private final LocalDate firstDay;

  private final LocalDate lastDay;

  private final Instant detectedAt;

  /** Captures a source omission pattern without recalculating analytics. */
  public AdherenceInsight(
      String medicationId,
      String medicationName,
      int omissionDays,
      LocalDate firstDay,
      LocalDate lastDay,
      Instant detectedAt) {
    requireText(medicationId, "medicationId");
    requireText(medicationName, "medicationName");
    Objects.requireNonNull(firstDay, "firstDay");
    Objects.requireNonNull(lastDay, "lastDay");
    Objects.requireNonNull(detectedAt, "detectedAt");
    if (lastDay.isBefore(firstDay)
        || omissionDays < 1
        || omissionDays > ChronoUnit.DAYS.between(firstDay, lastDay) + 1)
      throw new IllegalArgumentException("omission days must fit the source period");
    this.medicationId = medicationId;
    this.medicationName = medicationName;
    this.omissionDays = omissionDays;
    this.firstDay = firstDay;
    this.lastDay = lastDay;
    this.detectedAt = detectedAt;
  }

  /** Identifies a pattern by medication and inclusive source dates. */
  public boolean isSamePattern(String medicationId, LocalDate firstDay, LocalDate lastDay) {
    return this.medicationId.equals(medicationId)
        && this.firstDay.equals(firstDay)
        && this.lastDay.equals(lastDay);
  }

  /** Returns the stored identity, absent until first persistence. */
  public Long getId() {
    return id;
  }

  /** Returns the source medication reference. */
  public String getMedicationId() {
    return medicationId;
  }

  /** Returns the medication name recorded in the source evidence. */
  public String getMedicationName() {
    return medicationName;
  }

  /** Returns the number of distinct omission days in the source period. */
  public int getOmissionDays() {
    return omissionDays;
  }

  /** Returns the first inclusive date of the source period. */
  public LocalDate getFirstDay() {
    return firstDay;
  }

  /** Returns the last inclusive date of the source period. */
  public LocalDate getLastDay() {
    return lastDay;
  }

  /** Returns the source detection instant. */
  public Instant getDetectedAt() {
    return detectedAt;
  }

  /** Restores validated stored evidence, preserving original text and timestamps. */
  public static AdherenceInsight rehydrate(
      Long id,
      String medicationId,
      String medicationName,
      int omissionDays,
      LocalDate firstDay,
      LocalDate lastDay,
      Instant detectedAt) {
    if (id == null || id <= 0)
      throw new IllegalArgumentException("stored insight identity must be positive");
    var restored =
        new AdherenceInsight(
            medicationId, medicationName, omissionDays, firstDay, lastDay, detectedAt);
    restored.id = id;
    return restored;
  }
}
