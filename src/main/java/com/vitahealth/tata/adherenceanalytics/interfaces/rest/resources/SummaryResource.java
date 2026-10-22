package com.vitahealth.tata.adherenceanalytics.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/** Adherence history summary exposed by the REST boundary. */
@Schema(description = "Adherence summary of a period for the history screen")
public record SummaryResource(
    int periodDays,
    int scheduledCount,
    int adherencePercent,
    Integer adherenceChangePercent,
    int onTimePercent,
    Integer onTimeChangePercent,
    int lateCount,
    int omittedCount,
    List<TrendPointResource> trend,
    List<RecentIntakeResource> recentIntakes,
    SummaryPatternResource pattern) {
  /** Copies collection values at the transport boundary. */
  public SummaryResource {
    trend = List.copyOf(trend);
    recentIntakes = List.copyOf(recentIntakes);
  }
}
