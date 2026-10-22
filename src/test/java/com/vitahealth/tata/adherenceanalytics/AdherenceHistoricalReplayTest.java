package com.vitahealth.tata.adherenceanalytics;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.vitahealth.tata.adherenceanalytics.application.internal.commandservices.ConsolidateWeeklyPeriodCommandHandler;
import com.vitahealth.tata.adherenceanalytics.application.internal.commandservices.PersistAdherenceSnapshotCommandHandler;
import com.vitahealth.tata.adherenceanalytics.application.queryservices.AdherenceQueryService;
import com.vitahealth.tata.adherenceanalytics.domain.model.aggregates.AdherencePeriodSnapshot;
import com.vitahealth.tata.adherenceanalytics.domain.model.commands.ConsolidateWeeklyPeriodCommand;
import com.vitahealth.tata.adherenceanalytics.domain.model.valueobjects.AdherencePeriod;
import com.vitahealth.tata.adherenceanalytics.domain.repositories.AdherenceSnapshotRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Stored historical evidence must remain replayable independently of current source availability.
 */
class AdherenceHistoricalReplayTest {
  @Test
  void storedReplayDoesNotReadIntakeOrRepublish() {
    var queries = mock(AdherenceQueryService.class);
    var snapshots = mock(AdherenceSnapshotRepository.class);
    var persistence = mock(PersistAdherenceSnapshotCommandHandler.class);
    var from = Instant.parse("2026-10-01T00:00:00Z");
    var to = from.plusSeconds(604800);
    var period = new AdherencePeriod("adult", from, to, "UTC");
    var original =
        new AdherencePeriodSnapshot(
            period.snapshotId(), "adult", from, to, "Z", 0, 0, 0, List.of(), 3, to);
    when(snapshots.find(period.snapshotId())).thenReturn(Optional.of(original));
    var handler =
        new ConsolidateWeeklyPeriodCommandHandler(
            queries, snapshots, persistence, Clock.systemUTC());
    assertSame(
        original, handler.handle(new ConsolidateWeeklyPeriodCommand(" adult ", from, to, "UTC")));
    verifyNoInteractions(queries, persistence);
    assertEquals(period.snapshotId(), new AdherencePeriod("adult", from, to, "Z").snapshotId());
    assertThrows(
        IllegalArgumentException.class,
        () -> handler.handle(new ConsolidateWeeklyPeriodCommand("adult", to, from, "UTC")));
    verify(snapshots, times(1)).find(period.snapshotId());
  }
}
