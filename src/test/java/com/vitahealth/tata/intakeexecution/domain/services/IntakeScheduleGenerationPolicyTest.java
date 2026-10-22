package com.vitahealth.tata.intakeexecution.domain.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vitahealth.tata.intakeexecution.domain.model.valueobjects.ScheduleTimeZone;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Verifies explicit local calendar generation, UTC boundaries and daylight-saving resolution. */
class IntakeScheduleGenerationPolicyTest {
  @Test
  void resolvesBogotaLocalTimeToUtcWithoutUsingTheServerZone() {
    var result =
        generate(
            "America/Bogota", "2026-10-15T05:00:00Z", "2026-10-16T05:00:00Z", LocalTime.of(8, 0));
    assertEquals(List.of(Instant.parse("2026-10-15T13:00:00Z")), result);
  }

  @Test
  void springGapShiftsForwardByItsDurationAndFollowingDayKeepsTheLocalClock() {
    assertEquals(
        List.of(Instant.parse("2026-03-08T07:30:00Z"), Instant.parse("2026-03-09T06:30:00Z")),
        generate(
            "America/New_York",
            "2026-03-08T05:00:00Z",
            "2026-03-10T04:00:00Z",
            LocalTime.of(2, 30)));
  }

  @Test
  void autumnOverlapUsesEarlierOffsetOnlyOnce() {
    assertEquals(
        List.of(Instant.parse("2026-11-01T05:30:00Z")),
        generate(
            "America/New_York",
            "2026-11-01T04:00:00Z",
            "2026-11-02T05:00:00Z",
            LocalTime.of(1, 30)));
    assertTrue(
        generate(
                "America/New_York",
                "2026-11-01T06:00:00Z",
                "2026-11-02T05:00:00Z",
                LocalTime.of(1, 30))
            .isEmpty());
  }

  @Test
  void handlesHalfHourDaylightSavingGapWithoutAssumingSixtyMinutes() {
    assertEquals(
        List.of(Instant.parse("2026-10-03T15:45:00Z")),
        generate(
            "Australia/Lord_Howe",
            "2026-10-03T13:30:00Z",
            "2026-10-04T13:00:00Z",
            LocalTime.of(2, 15)));
  }

  @Test
  void sevenLocalDatesCanSpan167Or169Hours() {
    var zone = new ScheduleTimeZone("America/New_York");
    var spring = Instant.parse("2026-03-06T05:00:00Z");
    var autumn = Instant.parse("2026-10-30T04:00:00Z");
    assertEquals(
        167,
        Duration.between(spring, IntakeScheduleGenerationPolicy.horizonEnd(spring, zone, 7))
            .toHours());
    assertEquals(
        169,
        Duration.between(autumn, IntakeScheduleGenerationPolicy.horizonEnd(autumn, zone, 7))
            .toHours());
  }

  @Test
  void normalizesDuplicateTimesAndKeepsHalfOpenInstantBounds() {
    var result =
        IntakeScheduleGenerationPolicy.dailyTimes(
            List.of(LocalTime.NOON, LocalTime.of(8, 0), LocalTime.NOON),
            new ScheduleTimeZone("UTC"),
            Instant.parse("2026-10-15T08:00:00Z"),
            Instant.parse("2026-10-15T12:00:00Z"));
    assertEquals(List.of(Instant.parse("2026-10-15T08:00:00Z")), result);
    assertThrows(UnsupportedOperationException.class, () -> result.clear());
  }

  @Test
  void rejectsDistinctPrescriptionsThatCollapseToOneInstantDuringAGap() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            IntakeScheduleGenerationPolicy.dailyTimes(
                List.of(LocalTime.of(2, 30), LocalTime.of(3, 30)),
                new ScheduleTimeZone("America/New_York"),
                Instant.parse("2026-03-08T05:00:00Z"),
                Instant.parse("2026-03-09T04:00:00Z")));
  }

  @Test
  void rejectsUnknownZonesRawOffsetsAndInvalidHorizons() {
    for (var id : List.of("unknown", "+05:00", "", "America/Unknown"))
      assertThrows(IllegalArgumentException.class, () -> new ScheduleTimeZone(id));
    var now = Instant.parse("2026-10-15T00:00:00Z");
    var utc = new ScheduleTimeZone("UTC");
    assertThrows(
        IllegalArgumentException.class,
        () -> IntakeScheduleGenerationPolicy.horizonEnd(now, utc, 0));
    assertThrows(
        IllegalArgumentException.class,
        () -> IntakeScheduleGenerationPolicy.dailyTimes(List.of(LocalTime.NOON), utc, now, now));
    assertThrows(
        IllegalArgumentException.class,
        () -> IntakeScheduleGenerationPolicy.dailyTimes(List.of(), utc, now, now.plusSeconds(1)));
  }

  @Test
  void elapsedIntervalsPreserveTheirAnchorAcrossDaylightSavingAndHorizonExtensions() {
    var anchor = Instant.parse("2026-03-08T00:30:00Z");
    var to = anchor.plus(Duration.ofDays(2));
    var whole = IntakeScheduleGenerationPolicy.fixedInterval(anchor, 8, anchor, to);
    var split = anchor.plusSeconds(13 * 3600);
    var combined =
        new ArrayList<>(IntakeScheduleGenerationPolicy.fixedInterval(anchor, 8, anchor, split));
    combined.addAll(IntakeScheduleGenerationPolicy.fixedInterval(anchor, 8, split, to));
    assertEquals(whole, combined);
    assertEquals(6, whole.size());
    for (int i = 1; i < whole.size(); i++)
      assertEquals(Duration.ofHours(8), Duration.between(whole.get(i - 1), whole.get(i)));
    assertThrows(UnsupportedOperationException.class, () -> whole.clear());
  }

  @Test
  void intervalBoundsExcludeEndAndNeverGenerateBeforeTheFirstPrescription() {
    var anchor = Instant.parse("2026-10-21T13:00:00.123456Z");
    assertTrue(
        IntakeScheduleGenerationPolicy.fixedInterval(anchor, 12, anchor.minusSeconds(3600), anchor)
            .isEmpty());
    assertEquals(
        List.of(anchor),
        IntakeScheduleGenerationPolicy.fixedInterval(
            anchor, 12, anchor.minusSeconds(3600), anchor.plus(Duration.ofHours(12))));
    assertEquals(
        List.of(anchor.plus(Duration.ofHours(12))),
        IntakeScheduleGenerationPolicy.fixedInterval(
            anchor, 12, anchor.plusNanos(1), anchor.plus(Duration.ofHours(24))));
    assertEquals(
        List.of(Instant.MAX.minusSeconds(1)),
        IntakeScheduleGenerationPolicy.fixedInterval(
            Instant.MAX.minusSeconds(1), 8, Instant.MAX.minusSeconds(1), Instant.MAX));
  }

  @Test
  void intervalBeyondTheInstantLimitProducesNoOutOfRangeOccurrence() {
    assertTrue(
        IntakeScheduleGenerationPolicy.fixedInterval(
                Instant.MAX.minusSeconds(2), 8, Instant.MAX.minusSeconds(1), Instant.MAX)
            .isEmpty());
  }

  @Test
  void intervalGenerationRejectsUnsupportedOrUnboundedRequests() {
    var now = Instant.parse("2026-10-21T00:00:00Z");
    assertThrows(
        IllegalArgumentException.class,
        () -> IntakeScheduleGenerationPolicy.fixedInterval(now, 6, now, now.plusSeconds(1)));
    assertThrows(
        IllegalArgumentException.class,
        () -> IntakeScheduleGenerationPolicy.fixedInterval(now, 8, now, now));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            IntakeScheduleGenerationPolicy.fixedInterval(
                now, 12, now, now.plus(Duration.ofDays(9))));
  }

  private static List<Instant> generate(String zone, String from, String to, LocalTime time) {
    return IntakeScheduleGenerationPolicy.dailyTimes(
        List.of(time), new ScheduleTimeZone(zone), Instant.parse(from), Instant.parse(to));
  }
}
