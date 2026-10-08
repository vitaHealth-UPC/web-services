package com.tata.adherenceanalytics.application.queryservices;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

public interface AdherenceViewQueryService {
    int MAX_PERIOD_DAYS = 31;
    public enum TimeBand { MORNING, AFTERNOON, EVENING, NIGHT }
    public record TrendPoint(LocalDate date, int adherencePercent) {}
    public record RecentIntake(Instant scheduledAt, String medicationName, String status, Integer minutesLate) {}
    public record PatternSummary(TimeBand timeBand, int omittedCount, int lateCount) {}
    public record Summary(int periodDays, int scheduledCount, int adherencePercent, Integer adherenceChangePercent,
                          int onTimePercent, Integer onTimeChangePercent, int lateCount, int omittedCount,
                          List<TrendPoint> trend, List<RecentIntake> recentIntakes, PatternSummary pattern) {}
    public record InsightPattern(String type, TimeBand timeBand, int omittedCount, int lateCount,
                                 int fromHour, int toHour) {}
    public record Insights(int periodDays, InsightPattern pattern, List<List<Double>> concentration,
                           List<String> recommendations) {}
    Optional<Summary> summary(String olderAdultId, int periodDays, ZoneId zone, Instant now);
    Optional<Insights> insights(String olderAdultId, int periodDays, ZoneId zone, Instant now);
}
