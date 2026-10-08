package com.tata.adherenceanalytics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tata.adherenceanalytics.application.queryservices.AdherenceViewQueryService;
import com.tata.adherenceanalytics.application.queryservices.AdherenceViewQueryService.TimeBand;
import com.tata.adherenceanalytics.application.internal.outboundservices.IntakeRecordPort;
import com.tata.adherenceanalytics.application.internal.outboundservices.IntakeRecordPort.IntakeRecord;
import com.tata.adherenceanalytics.application.internal.outboundservices.IntakeRecordPort.Status;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class AdherenceViewQueryServiceTest {
    static final ZoneId LIMA = ZoneId.of("America/Lima");
    /** Tuesday 6 October 2026, 12:00 in Lima. */
    static final Instant NOW = Instant.parse("2026-10-06T17:00:00Z");

    final List<IntakeRecord> stored = new ArrayList<>();
    final IntakeRecordPort port = (owner, from, to) -> stored.stream()
            .filter(record -> !record.scheduledAt().isBefore(from) && record.scheduledAt().isBefore(to)).toList();
    final AdherenceViewQueryService service = new com.tata.adherenceanalytics.application.internal.queryservices.AdherenceViewQueryServiceImpl(port);

    private void add(Instant scheduledAt, Status status) {
        Instant confirmedAt = status == Status.CONFIRMED ? scheduledAt
                : status == Status.LATE ? scheduledAt.plus(Duration.ofMinutes(24)) : null;
        stored.add(new IntakeRecord("Losartán", scheduledAt, confirmedAt, status));
    }

    private void add(int times, Instant scheduledAt, Status status) {
        for (int i = 0; i < times; i++) add(scheduledAt.minusSeconds(i * 60L), status);
    }

    @Test
    void summaryIsEmptyWhenThereAreOnlyPendingIntakes() {
        add(NOW.minus(Duration.ofDays(1)), Status.PENDING);
        assertThat(service.summary("owner", 30, LIMA, NOW)).isEmpty();
    }

    @Test
    void summaryCalculatesPercentagesCountsAndChangeAgainstThePreviousPeriod() {
        Instant current = NOW.minus(Duration.ofDays(3));
        add(6, current, Status.CONFIRMED);
        add(2, current, Status.LATE);
        add(2, current, Status.OMITTED);
        Instant previous = NOW.minus(Duration.ofDays(10));
        add(5, previous, Status.CONFIRMED);
        add(1, previous, Status.LATE);
        add(4, previous, Status.OMITTED);

        var summary = service.summary("owner", 7, LIMA, NOW).orElseThrow();

        assertThat(summary.scheduledCount()).isEqualTo(10);
        assertThat(summary.adherencePercent()).isEqualTo(80);
        assertThat(summary.onTimePercent()).isEqualTo(60);
        assertThat(summary.lateCount()).isEqualTo(2);
        assertThat(summary.omittedCount()).isEqualTo(2);
        assertThat(summary.adherenceChangePercent()).isEqualTo(20);
        assertThat(summary.onTimeChangePercent()).isEqualTo(10);
    }

    @Test
    void changeIsAbsentWhenThePreviousPeriodHasNoEvidence() {
        add(2, NOW.minus(Duration.ofDays(1)), Status.CONFIRMED);
        var summary = service.summary("owner", 7, LIMA, NOW).orElseThrow();
        assertThat(summary.adherenceChangePercent()).isNull();
        assertThat(summary.onTimeChangePercent()).isNull();
    }

    @Test
    void recentIntakesAreNewestFirstAndLateOnesCarryTheMinutesLate() {
        add(NOW.minus(Duration.ofHours(30)), Status.OMITTED);
        add(NOW.minus(Duration.ofHours(20)), Status.LATE);
        add(NOW.minus(Duration.ofHours(4)), Status.CONFIRMED);
        add(NOW.minus(Duration.ofHours(40)), Status.CONFIRMED);

        var recent = service.summary("owner", 7, LIMA, NOW).orElseThrow().recentIntakes();

        assertThat(recent).hasSize(3);
        assertThat(recent.get(0).status()).isEqualTo("CONFIRMED");
        assertThat(recent.get(0).minutesLate()).isNull();
        assertThat(recent.get(1).status()).isEqualTo("LATE");
        assertThat(recent.get(1).minutesLate()).isEqualTo(24);
        assertThat(recent.get(2).status()).isEqualTo("OMITTED");
    }

    @Test
    void trendHasOnePointPerBucketWithEvidenceInChronologicalOrder() {
        add(2, NOW.minus(Duration.ofDays(6)).plus(Duration.ofHours(12)), Status.CONFIRMED);
        add(NOW.minus(Duration.ofHours(1)), Status.OMITTED);
        add(NOW.minus(Duration.ofHours(2)), Status.CONFIRMED);

        var trend = service.summary("owner", 7, LIMA, NOW).orElseThrow().trend();

        assertThat(trend).hasSize(2);
        assertThat(trend.get(0).adherencePercent()).isEqualTo(100);
        assertThat(trend.get(1).adherencePercent()).isEqualTo(50);
        assertThat(trend.get(0).date()).isBefore(trend.get(1).date());
    }

    /** 7:30 p. m. in Lima is 00:30 UTC of the next day. */
    private Instant eveningOf(int daysAgo) {
        return NOW.minus(Duration.ofDays(daysAgo)).atZone(LIMA).toLocalDate().atTime(19, 30).atZone(LIMA).toInstant();
    }

    @Test
    void patternAppearsWhenIssuesConcentrateInOneTimeBand() {
        add(eveningOf(1), Status.OMITTED);
        add(eveningOf(2), Status.OMITTED);
        add(eveningOf(3), Status.LATE);
        add(eveningOf(4), Status.LATE);
        add(NOW.minus(Duration.ofDays(5)), Status.CONFIRMED);

        var pattern = service.summary("owner", 30, LIMA, NOW).orElseThrow().pattern();

        assertThat(pattern).isNotNull();
        assertThat(pattern.timeBand()).isEqualTo(TimeBand.EVENING);
        assertThat(pattern.omittedCount()).isEqualTo(2);
        assertThat(pattern.lateCount()).isEqualTo(2);
    }

    @Test
    void noPatternWhenIssuesAreFewOrSpreadAcrossTheDay() {
        add(eveningOf(1), Status.OMITTED);
        add(eveningOf(2), Status.LATE);
        assertThat(service.summary("owner", 30, LIMA, NOW).orElseThrow().pattern()).isNull();
        for (int day = 3; day <= 4; day++) {
            add(NOW.minus(Duration.ofDays(day)).atZone(LIMA).toLocalDate().atTime(8, 0).atZone(LIMA).toInstant(),
                    Status.OMITTED);
            add(NOW.minus(Duration.ofDays(day)).atZone(LIMA).toLocalDate().atTime(14, 0).atZone(LIMA).toInstant(),
                    Status.OMITTED);
        }
        assertThat(service.summary("owner", 30, LIMA, NOW).orElseThrow().pattern()).isNull();
    }

    @Test
    void insightsRequireRecurringIssuesAndNeverSuggestMedicalChanges() {
        add(eveningOf(1), Status.OMITTED);
        add(eveningOf(2), Status.OMITTED);
        assertThat(service.insights("owner", 30, LIMA, NOW)).isEmpty();

        add(eveningOf(3), Status.LATE);
        add(eveningOf(4), Status.OMITTED);
        var insights = service.insights("owner", 30, LIMA, NOW).orElseThrow();

        assertThat(insights.pattern().type()).isEqualTo("OMISSION");
        assertThat(insights.pattern().timeBand()).isEqualTo(TimeBand.EVENING);
        assertThat(insights.pattern().omittedCount()).isEqualTo(3);
        assertThat(insights.pattern().lateCount()).isEqualTo(1);
        assertThat(insights.pattern().fromHour()).isEqualTo(19);
        assertThat(insights.pattern().toHour()).isEqualTo(20);
        assertThat(insights.recommendations())
                .containsExactly("ADJUST_REMINDER", "REVIEW_SCHEDULE", "FOLLOW_UP_ONE_WEEK");
        assertThat(insights.concentration()).hasSize(3).allSatisfy(row -> assertThat(row).hasSize(7));
        assertThat(insights.concentration().stream().flatMap(List::stream))
                .allSatisfy(value -> assertThat(value).isBetween(0.2, 0.8));
    }

    @Test
    void rejectsInvalidOwnerOrPeriod() {
        assertThatThrownBy(() -> service.summary(" ", 30, LIMA, NOW)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.summary("owner", 0, LIMA, NOW)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.insights("owner", 32, LIMA, NOW)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void multipleIssuesOnOneDayDoNotEstablishRecurrence() {
        add(4, eveningOf(1), Status.OMITTED);
        assertThat(service.insights("owner", 30, LIMA, NOW)).isEmpty();
        assertThat(service.summary("owner", 30, LIMA, NOW).orElseThrow().pattern()).isNull();
    }

    @Test
    void dominantBandRequiresThreeDistinctDaysOfEvidence() {
        add(eveningOf(1), Status.OMITTED);
        add(eveningOf(2), Status.LATE);
        add(NOW.minus(Duration.ofDays(3)).atZone(LIMA).toLocalDate().atTime(8, 0).atZone(LIMA).toInstant(), Status.LATE);
        assertThat(service.insights("owner", 30, LIMA, NOW)).isEmpty();
        add(eveningOf(3), Status.LATE);
        assertThat(service.insights("owner", 30, LIMA, NOW)).isPresent();
    }
}
