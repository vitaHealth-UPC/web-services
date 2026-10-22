package com.vitahealth.tata.adherenceanalytics.domain.model.valueobjects;

/** Definitive adherence counts with shared denominator and percentage rules. */
public record AdherenceCounts(int onTime, int late, int omitted) {
  /** Rejects negative or overflowing evidence before percentages are calculated. */
  public AdherenceCounts {
    if (onTime < 0
        || late < 0
        || omitted < 0
        || (long) onTime + late + omitted > Integer.MAX_VALUE) {
      throw new IllegalArgumentException(
          "adherence counts must be nonnegative and fit in an integer");
    }
  }

  /** Includes both on-time and late confirmations. */
  public int confirmed() {
    return onTime + late;
  }

  /** Excludes pending schedules from the definitive denominator. */
  public int total() {
    return confirmed() + omitted;
  }

  /** Returns the exact adherence percentage, or zero for empty evidence. */
  public double percentage() {
    return total() == 0 ? 0d : confirmed() * 100d / total();
  }

  /** Returns the whole-number adherence percentage used by screen summaries. */
  public int roundedAdherencePercent() {
    return (int) Math.round(percentage());
  }

  /** Returns the whole-number on-time percentage used by screen summaries. */
  public int roundedOnTimePercent() {
    return total() == 0 ? 0 : (int) Math.round(onTime * 100d / total());
  }
}
