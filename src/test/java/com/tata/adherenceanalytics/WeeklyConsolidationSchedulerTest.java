package com.tata.adherenceanalytics;

import com.tata.adherenceanalytics.application.internal.commandservices.ConsolidateWeeklyPeriodCommandHandler;
import com.tata.adherenceanalytics.application.internal.outboundservices.IntakeOutcomePort;
import com.tata.adherenceanalytics.infrastructure.scheduling.WeeklyConsolidationScheduler;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;

class WeeklyConsolidationSchedulerTest {
    @Test void calendarWeekKeepsMidnightAcrossDaylightSaving() {
        var outcomes = mock(IntakeOutcomePort.class);
        var handler = mock(ConsolidateWeeklyPeriodCommandHandler.class);
        var zone = ZoneId.of("America/New_York");
        var now = Instant.parse("2026-03-10T12:00:00Z");
        var day = now.atZone(zone).toLocalDate();
        var from = day.minusDays(7).atStartOfDay(zone).toInstant();
        var to = day.atStartOfDay(zone).toInstant();
        when(outcomes.olderAdultIdsWithOutcomes(from, to)).thenReturn(List.of("adult"));
        new WeeklyConsolidationScheduler(handler, outcomes, zone.getId()).consolidateWeekEnding(now);
        verify(handler).handle("adult", from, to, zone.getId());
    }
}
