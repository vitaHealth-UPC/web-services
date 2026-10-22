package com.vitahealth.tata.adherenceanalytics;

import static org.junit.jupiter.api.Assertions.*;

import com.vitahealth.tata.adherenceanalytics.domain.model.valueobjects.TimeBand;
import com.vitahealth.tata.adherenceanalytics.domain.services.AdherenceTimeBandPolicy;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Verifies calendar classification and evidence requirements independently of query transport. */
class AdherenceTimeBandPolicyTest {
  @Test
  void classifiesEveryLocalHourAndRejectsInvalidHours() {
    for (int hour = 0; hour < 24; hour++) {
      var expected =
          hour < 5 || hour >= 22
              ? TimeBand.NIGHT
              : hour < 12 ? TimeBand.MORNING : hour < 18 ? TimeBand.AFTERNOON : TimeBand.EVENING;
      assertEquals(expected, TimeBand.atHour(hour));
    }
    assertThrows(IllegalArgumentException.class, () -> TimeBand.atHour(-1));
    assertThrows(IllegalArgumentException.class, () -> TimeBand.atHour(24));
  }

  @Test
  void usesRequestedZoneAndRequiresDistinctCalendarDays() {
    var first = Instant.parse("2026-10-01T01:00:00Z");
    var zone = ZoneId.of("America/Lima");
    assertTrue(
        AdherenceTimeBandPolicy.dominantBand(
                List.of(first, first.plusSeconds(60), first.plusSeconds(120)), zone)
            .isEmpty());
    var days = List.of(first, first.plusSeconds(86400), first.plusSeconds(172800));
    assertEquals(TimeBand.EVENING, AdherenceTimeBandPolicy.dominantBand(days, zone).orElseThrow());
    assertEquals(
        TimeBand.NIGHT, AdherenceTimeBandPolicy.dominantBand(days, ZoneId.of("UTC")).orElseThrow());
  }
}
