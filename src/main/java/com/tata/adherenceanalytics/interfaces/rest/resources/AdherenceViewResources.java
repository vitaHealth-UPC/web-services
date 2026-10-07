package com.tata.adherenceanalytics.interfaces.rest.resources;

import com.tata.adherenceanalytics.application.queryservices.AdherenceViewQueryService;
import com.tata.adherenceanalytics.application.queryservices.AdherenceViewQueryService.Insights;
import com.tata.adherenceanalytics.application.queryservices.AdherenceViewQueryService.Summary;
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

    }

    public record TrendPointResource(LocalDate date, int adherencePercent) {}

    @Schema(description = "status is CONFIRMED (on time), LATE or OMITTED; minutesLate only applies to LATE")
    public record RecentIntakeResource(Instant scheduledAt, String medicationName, String status, Integer minutesLate) {}

    @Schema(description = "timeBand is MORNING, AFTERNOON, EVENING or NIGHT")
    public record SummaryPatternResource(String timeBand, int omittedCount, int lateCount) {}

    @Schema(description = "Recurring omission or lateness pattern with follow-up recommendation codes")
    public record InsightsResource(int periodDays, InsightPatternResource pattern, List<List<Double>> concentration,
                                   List<String> recommendations) {

    }

    @Schema(description = "type is OMISSION or LATENESS; hours are local, from inclusive to exclusive")
    public record InsightPatternResource(String type, String timeBand, int omittedCount, int lateCount,
                                         int fromHour, int toHour) {}
}
