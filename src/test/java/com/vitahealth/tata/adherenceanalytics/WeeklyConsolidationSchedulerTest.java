package com.vitahealth.tata.adherenceanalytics;

import static org.mockito.Mockito.*;

import com.vitahealth.tata.adherenceanalytics.application.internal.commandservices.ConsolidateWeeklyPeriodCommandHandler;
import com.vitahealth.tata.adherenceanalytics.application.internal.outboundservices.IntakeOutcomePort;
import com.vitahealth.tata.adherenceanalytics.domain.model.commands.ConsolidateWeeklyPeriodCommand;
import com.vitahealth.tata.adherenceanalytics.infrastructure.scheduling.WeeklyConsolidationScheduler;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;

class WeeklyConsolidationSchedulerTest {
  @Test
  void calendarWeekKeepsMidnightAcrossDaylightSaving() {
    var outcomes = mock(IntakeOutcomePort.class);
    var handler = mock(ConsolidateWeeklyPeriodCommandHandler.class);
    var zone = ZoneId.of("America/New_York");
    var now = Instant.parse("2026-03-10T12:00:00Z");
    var day = now.atZone(zone).toLocalDate();
    var from = day.minusDays(7).atStartOfDay(zone).toInstant();
    var to = day.atStartOfDay(zone).toInstant();
    when(outcomes.olderAdultIdsWithOutcomes(from, to, null, 100)).thenReturn(List.of("adult"));
    new WeeklyConsolidationScheduler(handler, outcomes, zone.getId(), Clock.systemUTC())
        .consolidateWeekEnding(now);
    verify(handler).handle(new ConsolidateWeeklyPeriodCommand("adult", from, to, zone.getId()));
  }

  @Test
  void continuesAcrossOwnerPagesAfterOneConsolidationFails() {
    var outcomes = mock(IntakeOutcomePort.class);
    var handler = mock(ConsolidateWeeklyPeriodCommandHandler.class);
    var now = Instant.parse("2026-10-08T12:00:00Z");
    var from = Instant.parse("2026-10-01T00:00:00Z");
    var to = Instant.parse("2026-10-08T00:00:00Z");
    when(outcomes.olderAdultIdsWithOutcomes(from, to, null, 100)).thenReturn(List.of("adult-a"));
    when(outcomes.olderAdultIdsWithOutcomes(from, to, "adult-a", 100))
        .thenReturn(List.of("adult-b"));
    when(outcomes.olderAdultIdsWithOutcomes(from, to, "adult-b", 100)).thenReturn(List.of());
    doThrow(new IllegalStateException("controlled owner failure"))
        .when(handler)
        .handle(new ConsolidateWeeklyPeriodCommand("adult-a", from, to, "UTC"));
    new WeeklyConsolidationScheduler(handler, outcomes, "UTC", Clock.systemUTC())
        .consolidateWeekEnding(now);
    verify(handler).handle(new ConsolidateWeeklyPeriodCommand("adult-b", from, to, "UTC"));
    verify(outcomes).olderAdultIdsWithOutcomes(from, to, "adult-b", 100);
  }
}
