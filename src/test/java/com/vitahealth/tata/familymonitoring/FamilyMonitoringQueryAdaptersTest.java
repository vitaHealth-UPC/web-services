package com.vitahealth.tata.familymonitoring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vitahealth.tata.adherenceanalytics.interfaces.acl.AdherenceContextFacade;
import com.vitahealth.tata.familymonitoring.application.exceptions.InvalidMonitoringSourceException;
import com.vitahealth.tata.familymonitoring.infrastructure.acl.AdherenceSummaryAdapter;
import com.vitahealth.tata.familymonitoring.infrastructure.acl.IntakeHistoryAdapter;
import com.vitahealth.tata.intakeexecution.interfaces.acl.IntakeContextFacade;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Verifies rolling query boundaries against the application clock rather than wall time. */
class FamilyMonitoringQueryAdaptersTest {
  private static final Instant NOW = Instant.parse("2026-10-18T12:34:56Z");
  private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);

  @Test
  void invalidAnalyticsCountsAreNotExposedAsAValidCaregiverSummary() {
    var source = mock(AdherenceContextFacade.class);
    // A deliberately corrupt provider bypasses the owning facade's constructor invariant.
    var invalid = mock(AdherenceContextFacade.WeeklyMetrics.class);
    when(invalid.confirmedIntakes()).thenReturn(5);
    when(invalid.totalIntakes()).thenReturn(4);
    when(source.weekly("adult", NOW.minus(Duration.ofDays(7)), NOW)).thenReturn(invalid);
    var failure =
        assertThrows(
            InvalidMonitoringSourceException.class,
            () -> new AdherenceSummaryAdapter(source, clock).getWeeklySummary("adult"));
    assertInstanceOf(IllegalArgumentException.class, failure.getCause());
  }

  @Test
  void historyUsesTheRequestedElapsedDaysEndingAtTheApplicationClock() {
    var source = mock(IntakeContextFacade.class);
    var from = NOW.minus(Duration.ofDays(3));
    when(source.history("adult", from, NOW)).thenReturn(List.of());

    assertEquals(List.of(), new IntakeHistoryAdapter(source, clock).getRecentIntakes("adult", 3));

    verify(source).history("adult", from, NOW);
  }

  @Test
  void weeklySummaryUsesTheSameClockAndPreservesSourceCounts() {
    var source = mock(AdherenceContextFacade.class);
    var from = NOW.minus(Duration.ofDays(7));
    when(source.weekly("adult", from, NOW))
        .thenReturn(new AdherenceContextFacade.WeeklyMetrics(4, 6));

    var summary = new AdherenceSummaryAdapter(source, clock).getWeeklySummary("adult");

    assertEquals(4, summary.confirmedIntakes());
    assertEquals(6, summary.totalIntakes());
    verify(source).weekly("adult", from, NOW);
  }
}
