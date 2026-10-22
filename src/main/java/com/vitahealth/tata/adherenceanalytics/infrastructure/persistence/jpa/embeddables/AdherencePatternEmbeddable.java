package com.vitahealth.tata.adherenceanalytics.infrastructure.persistence.jpa.embeddables;

import jakarta.persistence.Embeddable;
import java.time.LocalDate;

/** Owned scalar evidence for one medication pattern in an immutable period snapshot. */
@Embeddable
public class AdherencePatternEmbeddable {
  private String medicationId;
  private int omissionDays;
  private LocalDate firstDay;
  private LocalDate lastDay;

  /** Required by JPA field access. */
  protected AdherencePatternEmbeddable() {}

  /** Captures the original medication, distinct omission days and calendar bounds. */
  public AdherencePatternEmbeddable(
      String medicationId, int omissionDays, LocalDate firstDay, LocalDate lastDay) {
    this.medicationId = medicationId;
    this.omissionDays = omissionDays;
    this.firstDay = firstDay;
    this.lastDay = lastDay;
  }

  /** Returns the source medication identity. */
  public String medicationId() {
    return medicationId;
  }

  /** Returns the distinct omitted calendar-day count. */
  public int omissionDays() {
    return omissionDays;
  }

  /** Returns the first omitted calendar day. */
  public LocalDate firstDay() {
    return firstDay;
  }

  /** Returns the last omitted calendar day. */
  public LocalDate lastDay() {
    return lastDay;
  }
}
