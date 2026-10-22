package com.vitahealth.tata.adherenceanalytics.domain.model.valueobjects;

/** Calendar time bands used to group adherence evidence in the requested zone. */
public enum TimeBand {
  MORNING,
  AFTERNOON,
  EVENING,
  NIGHT;

  /** Classifies a valid local clock hour using the shared adherence calendar boundaries. */
  public static TimeBand atHour(int hour) {
    if (hour < 0 || hour > 23) throw new IllegalArgumentException("hour must be between 0 and 23");
    if (hour >= 5 && hour < 12) return MORNING;
    if (hour >= 12 && hour < 18) return AFTERNOON;
    if (hour >= 18 && hour < 22) return EVENING;
    return NIGHT;
  }
}
