package com.tata.adherenceanalytics.interfaces.rest.transform;
import com.tata.adherenceanalytics.application.queryservices.AdherenceViewQueryService;
import com.tata.adherenceanalytics.application.queryservices.AdherenceViewQueryService.Insights;
import com.tata.adherenceanalytics.application.queryservices.AdherenceViewQueryService.Summary;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import com.tata.adherenceanalytics.interfaces.rest.resources.AdherenceViewResources;
import com.tata.adherenceanalytics.interfaces.rest.resources.AdherenceViewResources.*;
public final class AdherenceViewResourceAssembler {
 private AdherenceViewResourceAssembler() {}
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
        public static InsightsResource from(Insights insights) {
            var pattern = insights.pattern();
            return new InsightsResource(insights.periodDays(),
                    new InsightPatternResource(pattern.type(), pattern.timeBand().name(), pattern.omittedCount(),
                            pattern.lateCount(), pattern.fromHour(), pattern.toHour()),
                    insights.concentration(), insights.recommendations());
        }
}
