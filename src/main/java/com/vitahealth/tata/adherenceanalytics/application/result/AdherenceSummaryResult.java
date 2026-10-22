package com.vitahealth.tata.adherenceanalytics.application.result;

import java.util.List;

/** Immutable adherence summary and comparisons for the requested period. */
public record AdherenceSummaryResult(
    int periodDays,
    int scheduledCount,
    int adherencePercent,
    Integer adherenceChangePercent,
    int onTimePercent,
    Integer onTimeChangePercent,
    int lateCount,
    int omittedCount,
    List<AdherenceTrendPointResult> trend,
    List<RecentAdherenceIntakeResult> recentIntakes,
    AdherencePatternSummaryResult pattern) {
  /** Copies collections so callers cannot alter the query result after construction. */
  public AdherenceSummaryResult {
    trend = List.copyOf(trend);
    recentIntakes = List.copyOf(recentIntakes);
  }
}
