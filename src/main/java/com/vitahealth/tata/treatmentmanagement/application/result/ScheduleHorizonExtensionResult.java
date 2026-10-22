package com.vitahealth.tata.treatmentmanagement.application.result;

/**
 * Reports committed publications, no-longer-eligible candidates and isolated failed transactions.
 */
public record ScheduleHorizonExtensionResult(int published, int skipped, int failed) {
  /** Rejects impossible negative batch counts. */
  public ScheduleHorizonExtensionResult {
    if (published < 0 || skipped < 0 || failed < 0)
      throw new IllegalArgumentException("horizon counts must not be negative");
  }
}
