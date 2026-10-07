package com.tata.adherenceanalytics.interfaces.rest;

import com.tata.adherenceanalytics.application.AdherenceViewQueryService.Insights;
import com.tata.adherenceanalytics.application.AdherenceViewQueryService.Summary;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** REST resources for the adherence screens; they are not domain objects. */
public final class AdherenceViewResources {
    private AdherenceViewResources() {}

    @Schema(description = "Adherence summary of a period for the history screen")
    public record SummaryResource(int periodDays, int scheduledCount, int adherencePercent,
                                  Integer adherenceChangePercent, int onTimePercent, Integer onTimeChangePercent,
                                  int lateCount, int omittedCount, List<TrendPointResource> trend,
                                  List<RecentIntakeResource> recentIntakes, SummaryPatternResource pattern) {
        public static SummaryResource from(Summary summary) {
            return new SummaryResource(summary.periodDays(), summary.scheduledCount(), summary.adherencePercent(),
                    summary.adherenceChangePercent(), summary.onTimePercent(), summary.onTimeChangePercent(),
                    summary.lateCount(), summary.omittedCount(),
                    summary.trend().stream().map(point -> new TrendPointResource(point.date(), point.adherencePercent())).toList(),
                    summary.recentIntakes().stream().map(item -> new RecentIntakeResource(item.scheduledAt(),
                            item.medicationName(), item.status(), item.minutesLate())).toList(),
                    summary.pattern() == null ? null : new SummaryPatternResource(summary.pattern().timeBand().name(),
                            summary.pattern().omittedCount(), summary.pattern().lateCount()));
        }
    }

    public record TrendPointResource(LocalDate date, int adherencePercent) {}

    @Schema(description = "status is CONFIRMED (on time), LATE or OMITTED; minutesLate only applies to LATE")
    public record RecentIntakeResource(Instant scheduledAt, String medicationName, String status, Integer minutesLate) {}

    @Schema(description = "timeBand is MORNING, AFTERNOON, EVENING or NIGHT")
    public record SummaryPatternResource(String timeBand, int omittedCount, int lateCount) {}

    @Schema(description = "Recurring omission or lateness pattern with follow-up recommendation codes")
    public record InsightsResource(int periodDays, InsightPatternResource pattern, List<List<Double>> concentration,
                                   List<String> recommendations) {
        public static InsightsResource from(Insights insights) {
            var pattern = insights.pattern();
            return new InsightsResource(insights.periodDays(),
                    new InsightPatternResource(pattern.type(), pattern.timeBand().name(), pattern.omittedCount(),
                            pattern.lateCount(), pattern.fromHour(), pattern.toHour()),
                    insights.concentration(), insights.recommendations());
        }
    }

    @Schema(description = "type is OMISSION or LATENESS; hours are local, from inclusive to exclusive")
    public record InsightPatternResource(String type, String timeBand, int omittedCount, int lateCount,
                                         int fromHour, int toHour) {}
}
