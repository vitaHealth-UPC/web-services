package com.vitahealth.tata.adherenceanalytics.application.internal.queryservices;

import com.vitahealth.tata.adherenceanalytics.application.internal.outboundservices.IntakeRecordPort;
import com.vitahealth.tata.adherenceanalytics.application.queryservices.AdherenceViewQueryService;
import com.vitahealth.tata.adherenceanalytics.application.result.AdherenceInsightPatternResult;
import com.vitahealth.tata.adherenceanalytics.application.result.AdherenceInsightsResult;
import com.vitahealth.tata.adherenceanalytics.application.result.AdherencePatternSummaryResult;
import com.vitahealth.tata.adherenceanalytics.application.result.AdherenceSummaryResult;
import com.vitahealth.tata.adherenceanalytics.application.result.AdherenceTrendPointResult;
import com.vitahealth.tata.adherenceanalytics.application.result.RecentAdherenceIntakeResult;
import com.vitahealth.tata.adherenceanalytics.domain.model.queries.GetAdherenceViewQuery;
import com.vitahealth.tata.adherenceanalytics.domain.model.valueobjects.AdherenceCounts;
import com.vitahealth.tata.adherenceanalytics.domain.model.valueobjects.AdherenceHourRange;
import com.vitahealth.tata.adherenceanalytics.domain.model.valueobjects.AdherenceIntakeRecord;
import com.vitahealth.tata.adherenceanalytics.domain.model.valueobjects.AdherenceIntakeRecord.Status;
import com.vitahealth.tata.adherenceanalytics.domain.model.valueobjects.TimeBand;
import com.vitahealth.tata.adherenceanalytics.domain.services.AdherenceTimeBandPolicy;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read models for the adherence screens of the family app: the history summary and the
 * recommendations. Empty results mean insufficient evidence; they never invent figures or suggest
 * medical changes.
 */
@Service
@Transactional(readOnly = true)
public class AdherenceViewQueryServiceImpl implements AdherenceViewQueryService {
  private static final int TREND_BUCKETS = 7;
  private static final int RECENT_INTAKES = 3;
  private static final int MINIMUM_ISSUES_FOR_PATTERN = 3;

  private final IntakeRecordPort records;

  /** Injects the validated source evidence read port. */
  public AdherenceViewQueryServiceImpl(IntakeRecordPort records) {
    this.records = records;
  }

  /** Computes history metrics for a validated owner and period. */
  @Override
  public Optional<AdherenceSummaryResult> summary(GetAdherenceViewQuery query) {
    String olderAdultId = query.olderAdultId();
    int periodDays = query.periodDays();
    ZoneId zone = query.zone();
    Instant now = query.now();
    Instant from = now.minus(Duration.ofDays(periodDays));
    Instant previousFrom = from.minus(Duration.ofDays(periodDays));
    List<AdherenceIntakeRecord> all = records.find(olderAdultId.trim(), previousFrom, now);
    List<AdherenceIntakeRecord> current = definitive(all, from, now);
    if (current.isEmpty()) return Optional.empty();
    List<AdherenceIntakeRecord> previous = definitive(all, previousFrom, from);

    AdherenceCounts counts = count(current);
    AdherenceCounts before = count(previous);
    Integer adherenceChange =
        previous.isEmpty()
            ? null
            : counts.roundedAdherencePercent() - before.roundedAdherencePercent();
    Integer onTimeChange =
        previous.isEmpty() ? null : counts.roundedOnTimePercent() - before.roundedOnTimePercent();

    return Optional.of(
        new AdherenceSummaryResult(
            periodDays,
            counts.total(),
            counts.roundedAdherencePercent(),
            adherenceChange,
            counts.roundedOnTimePercent(),
            onTimeChange,
            counts.late(),
            counts.omitted(),
            trend(current, from, now, periodDays, zone),
            recent(current),
            pattern(current, zone)));
  }

  /** Computes recommendations only from sufficient recurring issue evidence. */
  @Override
  public Optional<AdherenceInsightsResult> insights(GetAdherenceViewQuery query) {
    String olderAdultId = query.olderAdultId();
    int periodDays = query.periodDays();
    ZoneId zone = query.zone();
    Instant now = query.now();
    Instant from = now.minus(Duration.ofDays(periodDays));
    List<AdherenceIntakeRecord> issues =
        definitive(records.find(olderAdultId.trim(), from, now), from, now).stream()
            .filter(record -> record.status() == Status.LATE || record.status() == Status.OMITTED)
            .toList();
    if (issues.size() < MINIMUM_ISSUES_FOR_PATTERN) return Optional.empty();
    TimeBand band = dominantBand(issues, zone);
    if (band == null) return Optional.empty();

    List<AdherenceIntakeRecord> inBand =
        issues.stream().filter(record -> bandOf(record, zone) == band).toList();
    int omitted = (int) inBand.stream().filter(record -> record.status() == Status.OMITTED).count();
    int late = inBand.size() - omitted;
    var hours =
        AdherenceHourRange.ofHours(
            inBand.stream().map(record -> hourOf(record, zone)).toList(), band);
    AdherenceInsightPatternResult pattern =
        new AdherenceInsightPatternResult(
            omitted >= late ? "OMISSION" : "LATENESS",
            band,
            omitted,
            late,
            hours.fromHour(),
            hours.toHour());
    return Optional.of(
        new AdherenceInsightsResult(
            periodDays,
            pattern,
            concentration(issues, zone),
            List.of("ADJUST_REMINDER", "REVIEW_SCHEDULE", "FOLLOW_UP_ONE_WEEK")));
  }

  private static List<AdherenceIntakeRecord> definitive(
      List<AdherenceIntakeRecord> all, Instant from, Instant to) {
    return all.stream()
        .filter(record -> record.status() != Status.PENDING)
        .filter(record -> !record.scheduledAt().isBefore(from) && record.scheduledAt().isBefore(to))
        .toList();
  }

  private static AdherenceCounts count(List<AdherenceIntakeRecord> records) {
    int onTime =
        (int) records.stream().filter(record -> record.status() == Status.CONFIRMED).count();
    int late = (int) records.stream().filter(record -> record.status() == Status.LATE).count();
    int omitted =
        (int) records.stream().filter(record -> record.status() == Status.OMITTED).count();
    return new AdherenceCounts(onTime, late, omitted);
  }

  private static List<AdherenceTrendPointResult> trend(
      List<AdherenceIntakeRecord> current, Instant from, Instant now, int periodDays, ZoneId zone) {
    long bucketSeconds = Math.max(1, Duration.ofDays(periodDays).toSeconds() / TREND_BUCKETS);
    Map<Integer, List<AdherenceIntakeRecord>> buckets = new TreeMap<>();
    for (AdherenceIntakeRecord record : current) {
      int index =
          (int)
              Math.min(
                  TREND_BUCKETS - 1,
                  Duration.between(from, record.scheduledAt()).toSeconds() / bucketSeconds);
      buckets.computeIfAbsent(index, key -> new ArrayList<>()).add(record);
    }
    List<AdherenceTrendPointResult> points = new ArrayList<>();
    buckets.forEach(
        (index, items) -> {
          Instant bucketEnd =
              index == TREND_BUCKETS - 1 ? now : from.plusSeconds(bucketSeconds * (index + 1));
          AdherenceCounts counts = count(items);
          points.add(
              new AdherenceTrendPointResult(
                  bucketEnd.minusSeconds(1).atZone(zone).toLocalDate(),
                  counts.roundedAdherencePercent()));
        });
    return points;
  }

  private static List<RecentAdherenceIntakeResult> recent(List<AdherenceIntakeRecord> current) {
    return current.stream()
        .sorted(Comparator.comparing(AdherenceIntakeRecord::scheduledAt).reversed())
        .limit(RECENT_INTAKES)
        .map(
            record ->
                new RecentAdherenceIntakeResult(
                    record.scheduledAt(),
                    record.medicationName(),
                    record.status().name(),
                    minutesLate(record)))
        .toList();
  }

  private static Integer minutesLate(AdherenceIntakeRecord record) {
    if (record.status() != Status.LATE || record.confirmedAt() == null) return null;
    return (int)
        Math.max(0, Duration.between(record.scheduledAt(), record.confirmedAt()).toMinutes());
  }

  private static AdherencePatternSummaryResult pattern(
      List<AdherenceIntakeRecord> current, ZoneId zone) {
    List<AdherenceIntakeRecord> issues =
        current.stream()
            .filter(record -> record.status() == Status.LATE || record.status() == Status.OMITTED)
            .toList();
    if (issues.size() < MINIMUM_ISSUES_FOR_PATTERN) return null;
    TimeBand band = dominantBand(issues, zone);
    if (band == null) return null;
    List<AdherenceIntakeRecord> inBand =
        issues.stream().filter(record -> bandOf(record, zone) == band).toList();
    int omitted = (int) inBand.stream().filter(record -> record.status() == Status.OMITTED).count();
    return new AdherencePatternSummaryResult(band, omitted, inBand.size() - omitted);
  }

  private static TimeBand dominantBand(List<AdherenceIntakeRecord> issues, ZoneId zone) {
    return AdherenceTimeBandPolicy.dominantBand(
            issues.stream().map(AdherenceIntakeRecord::scheduledAt).toList(), zone)
        .orElse(null);
  }

  private static TimeBand bandOf(AdherenceIntakeRecord record, ZoneId zone) {
    return TimeBand.atHour(hourOf(record, zone));
  }

  private static int hourOf(AdherenceIntakeRecord record, ZoneId zone) {
    return record.scheduledAt().atZone(zone).getHour();
  }

  /**
   * Three display rows (morning, afternoon, combined evening/night) by seven weekdays, as intensity
   * from 0.2 to 0.8.
   */
  private static List<List<Double>> concentration(List<AdherenceIntakeRecord> issues, ZoneId zone) {
    int[][] counts = new int[3][7];
    for (AdherenceIntakeRecord record : issues) {
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
