package com.tata.adherenceanalytics.application;

import com.tata.adherenceanalytics.application.ports.IntakeRecordPort;
import com.tata.adherenceanalytics.application.ports.IntakeRecordPort.IntakeRecord;
import com.tata.adherenceanalytics.application.ports.IntakeRecordPort.Status;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read models for the adherence screens of the family app: the history summary and the recommendations.
 * Empty results mean insufficient evidence; they never invent figures or suggest medical changes.
 */
@Service
@Transactional(readOnly = true)
public class AdherenceViewQueryService {
    public static final int MAX_PERIOD_DAYS = 31;
    private static final int TREND_BUCKETS = 7;
    private static final int RECENT_INTAKES = 3;
    private static final int MINIMUM_ISSUES_FOR_PATTERN = 3;
    private static final double MINIMUM_BAND_SHARE = 0.5;

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

    private record Counts(int total, int onTime, int late, int omitted) {
        int confirmed() { return onTime + late; }
    }

    private final IntakeRecordPort records;

    public AdherenceViewQueryService(IntakeRecordPort records) {
        this.records = records;
    }

    public Optional<Summary> summary(String olderAdultId, int periodDays, ZoneId zone, Instant now) {
        validate(olderAdultId, periodDays);
        Instant from = now.minus(Duration.ofDays(periodDays));
        Instant previousFrom = from.minus(Duration.ofDays(periodDays));
        List<IntakeRecord> all = records.find(olderAdultId.trim(), previousFrom, now);
        List<IntakeRecord> current = definitive(all, from, now);
        if (current.isEmpty()) return Optional.empty();
        List<IntakeRecord> previous = definitive(all, previousFrom, from);

        Counts counts = count(current);
        Counts before = count(previous);
        Integer adherenceChange = previous.isEmpty() ? null
                : percent(counts.confirmed(), counts.total()) - percent(before.confirmed(), before.total());
        Integer onTimeChange = previous.isEmpty() ? null
                : percent(counts.onTime(), counts.total()) - percent(before.onTime(), before.total());

        return Optional.of(new Summary(periodDays, counts.total(), percent(counts.confirmed(), counts.total()),
                adherenceChange, percent(counts.onTime(), counts.total()), onTimeChange, counts.late(),
                counts.omitted(), trend(current, from, now, periodDays, zone), recent(current),
                pattern(current, zone)));
    }

    public Optional<Insights> insights(String olderAdultId, int periodDays, ZoneId zone, Instant now) {
        validate(olderAdultId, periodDays);
        Instant from = now.minus(Duration.ofDays(periodDays));
        List<IntakeRecord> issues = definitive(records.find(olderAdultId.trim(), from, now), from, now).stream()
                .filter(record -> record.status() == Status.LATE || record.status() == Status.OMITTED)
                .toList();
        if (issues.size() < MINIMUM_ISSUES_FOR_PATTERN) return Optional.empty();
        TimeBand band = dominantBand(issues, zone);
        if (band == null) return Optional.empty();

        List<IntakeRecord> inBand = issues.stream().filter(record -> bandOf(record, zone) == band).toList();
        int omitted = (int) inBand.stream().filter(record -> record.status() == Status.OMITTED).count();
        int late = inBand.size() - omitted;
        int firstHour = inBand.stream().mapToInt(record -> hourOf(record, zone)).min().orElse(0);
        int lastHour = inBand.stream().mapToInt(record -> hourOf(record, zone)).max().orElse(0);
        InsightPattern pattern = new InsightPattern(omitted >= late ? "OMISSION" : "LATENESS", band, omitted, late,
                firstHour, Math.min(24, lastHour + 1));
        return Optional.of(new Insights(periodDays, pattern, concentration(issues, zone),
                List.of("ADJUST_REMINDER", "REVIEW_SCHEDULE", "FOLLOW_UP_ONE_WEEK")));
    }

    private static void validate(String olderAdultId, int periodDays) {
        if (olderAdultId == null || olderAdultId.isBlank())
            throw new IllegalArgumentException("older adult is required");
        if (periodDays < 1 || periodDays > MAX_PERIOD_DAYS)
            throw new IllegalArgumentException("period must be between one and " + MAX_PERIOD_DAYS + " days");
    }

    private static List<IntakeRecord> definitive(List<IntakeRecord> all, Instant from, Instant to) {
        return all.stream()
                .filter(record -> record.status() != Status.PENDING)
                .filter(record -> !record.scheduledAt().isBefore(from) && record.scheduledAt().isBefore(to))
                .toList();
    }

    private static Counts count(List<IntakeRecord> records) {
        int onTime = (int) records.stream().filter(record -> record.status() == Status.CONFIRMED).count();
        int late = (int) records.stream().filter(record -> record.status() == Status.LATE).count();
        int omitted = (int) records.stream().filter(record -> record.status() == Status.OMITTED).count();
        return new Counts(records.size(), onTime, late, omitted);
    }

    private static int percent(int part, int total) {
        return total == 0 ? 0 : (int) Math.round(part * 100.0 / total);
    }

    private static List<TrendPoint> trend(List<IntakeRecord> current, Instant from, Instant now, int periodDays,
                                          ZoneId zone) {
        long bucketSeconds = Math.max(1, Duration.ofDays(periodDays).toSeconds() / TREND_BUCKETS);
        Map<Integer, List<IntakeRecord>> buckets = new java.util.TreeMap<>();
        for (IntakeRecord record : current) {
            int index = (int) Math.min(TREND_BUCKETS - 1,
                    Duration.between(from, record.scheduledAt()).toSeconds() / bucketSeconds);
            buckets.computeIfAbsent(index, key -> new ArrayList<>()).add(record);
        }
        List<TrendPoint> points = new ArrayList<>();
        buckets.forEach((index, items) -> {
            Instant bucketEnd = index == TREND_BUCKETS - 1 ? now : from.plusSeconds(bucketSeconds * (index + 1));
            Counts counts = count(items);
            points.add(new TrendPoint(bucketEnd.minusSeconds(1).atZone(zone).toLocalDate(),
                    percent(counts.confirmed(), counts.total())));
        });
        return points;
    }

    private static List<RecentIntake> recent(List<IntakeRecord> current) {
        return current.stream()
                .sorted(Comparator.comparing(IntakeRecord::scheduledAt).reversed())
                .limit(RECENT_INTAKES)
                .map(record -> new RecentIntake(record.scheduledAt(), record.medicationName(),
                        record.status().name(), minutesLate(record)))
                .toList();
    }

    private static Integer minutesLate(IntakeRecord record) {
        if (record.status() != Status.LATE || record.confirmedAt() == null) return null;
        return (int) Math.max(0, Duration.between(record.scheduledAt(), record.confirmedAt()).toMinutes());
    }

    private static PatternSummary pattern(List<IntakeRecord> current, ZoneId zone) {
        List<IntakeRecord> issues = current.stream()
                .filter(record -> record.status() == Status.LATE || record.status() == Status.OMITTED).toList();
        if (issues.size() < MINIMUM_ISSUES_FOR_PATTERN) return null;
        TimeBand band = dominantBand(issues, zone);
        if (band == null) return null;
        List<IntakeRecord> inBand = issues.stream().filter(record -> bandOf(record, zone) == band).toList();
        int omitted = (int) inBand.stream().filter(record -> record.status() == Status.OMITTED).count();
        return new PatternSummary(band, omitted, inBand.size() - omitted);
    }

    private static TimeBand dominantBand(List<IntakeRecord> issues, ZoneId zone) {
        Map<TimeBand, Integer> counts = new EnumMap<>(TimeBand.class);
        issues.forEach(record -> counts.merge(bandOf(record, zone), 1, Integer::sum));
        var dominant = counts.entrySet().stream().max(Map.Entry.comparingByValue()).orElse(null);
        if (dominant == null || dominant.getValue() < MINIMUM_ISSUES_FOR_PATTERN
                || dominant.getValue() < issues.size() * MINIMUM_BAND_SHARE) return null;
        // Several doses on one day do not establish a recurring routine.
        if (issues.stream().filter(record -> bandOf(record, zone) == dominant.getKey())
                .map(record -> record.scheduledAt().atZone(zone).toLocalDate()).distinct().count()
                < MINIMUM_ISSUES_FOR_PATTERN) return null;
        return dominant.getKey();
    }

    private static TimeBand bandOf(IntakeRecord record, ZoneId zone) {
        int hour = hourOf(record, zone);
        if (hour >= 5 && hour < 12) return TimeBand.MORNING;
        if (hour >= 12 && hour < 18) return TimeBand.AFTERNOON;
        if (hour >= 18 && hour < 22) return TimeBand.EVENING;
        return TimeBand.NIGHT;
    }

    private static int hourOf(IntakeRecord record, ZoneId zone) {
        return record.scheduledAt().atZone(zone).getHour();
    }

    /** Three time bands (morning, afternoon, evening and night) by seven weekdays, as intensity from 0.2 to 0.8. */
    private static List<List<Double>> concentration(List<IntakeRecord> issues, ZoneId zone) {
        int[][] counts = new int[3][7];
        for (IntakeRecord record : issues) {
            TimeBand band = bandOf(record, zone);
            int row = band == TimeBand.MORNING ? 0 : band == TimeBand.AFTERNOON ? 1 : 2;
            DayOfWeek day = record.scheduledAt().atZone(zone).getDayOfWeek();
            counts[row][day.getValue() - 1]++;
        }
        int max = 1;
        for (int[] row : counts) for (int value : row) max = Math.max(max, value);
        List<List<Double>> rows = new ArrayList<>();
        for (int[] row : counts) {
            List<Double> cells = new ArrayList<>();
            for (int value : row) cells.add(Math.round((0.2 + 0.6 * value / max) * 1000) / 1000.0);
            rows.add(List.copyOf(cells));
        }
        return List.copyOf(rows);
    }
}
