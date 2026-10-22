package com.vitahealth.tata.adherenceanalytics.interfaces.rest.transform;

import com.vitahealth.tata.adherenceanalytics.application.result.AdherenceSummaryResult;
import com.vitahealth.tata.adherenceanalytics.interfaces.rest.resources.RecentIntakeResource;
import com.vitahealth.tata.adherenceanalytics.interfaces.rest.resources.SummaryPatternResource;
import com.vitahealth.tata.adherenceanalytics.interfaces.rest.resources.SummaryResource;
import com.vitahealth.tata.adherenceanalytics.interfaces.rest.resources.TrendPointResource;

/** Maps adherence history results into the public summary resource. */
public final class AdherenceSummaryResourceAssembler {
  private AdherenceSummaryResourceAssembler() {}

  /** Preserves all result values while mapping domain labels to transport strings. */
  public static SummaryResource from(AdherenceSummaryResult summary) {
    return new SummaryResource(
        summary.periodDays(),
        summary.scheduledCount(),
        summary.adherencePercent(),
        summary.adherenceChangePercent(),
        summary.onTimePercent(),
        summary.onTimeChangePercent(),
        summary.lateCount(),
        summary.omittedCount(),
        summary.trend().stream()
            .map(point -> new TrendPointResource(point.date(), point.adherencePercent()))
            .toList(),
        summary.recentIntakes().stream()
            .map(
                item ->
                    new RecentIntakeResource(
                        item.scheduledAt(),
                        item.medicationName(),
                        item.status(),
                        item.minutesLate()))
            .toList(),
        summary.pattern() == null
            ? null
            : new SummaryPatternResource(
                summary.pattern().timeBand().name(),
                summary.pattern().omittedCount(),
                summary.pattern().lateCount()));
  }
}
