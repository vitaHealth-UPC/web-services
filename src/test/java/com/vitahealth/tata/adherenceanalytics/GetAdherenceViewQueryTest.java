package com.vitahealth.tata.adherenceanalytics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.vitahealth.tata.adherenceanalytics.domain.model.queries.GetAdherenceViewQuery;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

/**
 * Checks the common view query invariants before either read operation accesses source evidence.
 */
class GetAdherenceViewQueryTest {
  private final Instant now = Instant.parse("2026-10-20T00:00:00Z");
  private final ZoneId zone = ZoneId.of("America/Lima");

  @Test
  void normalizesOwnerAndAcceptsBothPeriodLimits() {
    assertEquals("adult", new GetAdherenceViewQuery(" adult ", 1, zone, now).olderAdultId());
    assertEquals(31, new GetAdherenceViewQuery("adult", 31, zone, now).periodDays());
  }

  @Test
  void rejectsIncompleteCalendarReferenceAndUnsupportedPeriods() {
    assertThrows(
        IllegalArgumentException.class, () -> new GetAdherenceViewQuery("adult", 7, null, now));
    assertThrows(
        IllegalArgumentException.class, () -> new GetAdherenceViewQuery("adult", 7, zone, null));
    assertThrows(
        IllegalArgumentException.class, () -> new GetAdherenceViewQuery(null, 7, zone, now));
    assertThrows(
        IllegalArgumentException.class, () -> new GetAdherenceViewQuery(" ", 7, zone, now));
    assertThrows(
        IllegalArgumentException.class, () -> new GetAdherenceViewQuery("adult", 0, zone, now));
    assertThrows(
        IllegalArgumentException.class, () -> new GetAdherenceViewQuery("adult", 32, zone, now));
  }
}
