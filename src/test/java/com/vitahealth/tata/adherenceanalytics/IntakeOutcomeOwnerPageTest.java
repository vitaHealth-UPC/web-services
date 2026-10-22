package com.vitahealth.tata.adherenceanalytics;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.vitahealth.tata.adherenceanalytics.application.exceptions.AdherenceSourceUnavailableException;
import com.vitahealth.tata.adherenceanalytics.application.exceptions.InvalidAdherenceSourceException;
import com.vitahealth.tata.adherenceanalytics.infrastructure.acl.IntakeEvidenceReader;
import com.vitahealth.tata.adherenceanalytics.infrastructure.acl.IntakeOutcomeAdapter;
import com.vitahealth.tata.intakeexecution.interfaces.acl.IntakeContextFacade;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Checks source participant pages before the scheduler advances its exclusive cursor. */
class IntakeOutcomeOwnerPageTest {
  private final IntakeContextFacade source = mock(IntakeContextFacade.class);
  private final IntakeOutcomeAdapter adapter =
      new IntakeOutcomeAdapter(source, new IntakeEvidenceReader(source));
  private final Instant from = Instant.parse("2026-10-01T00:00:00Z");
  private final Instant to = from.plusSeconds(604800);

  @Test
  void rejectsDuplicateBackwardBlankAndOversizedPages() {
    for (var invalid :
        List.of(
            List.of("adult-b", "adult-b"),
            List.of("adult-a"),
            List.of(" "),
            List.of("adult-b", "adult-c", "adult-d"))) {
      when(source.findOlderAdultIdsWithIntakes(from, to, "adult-a", 2)).thenReturn(invalid);
      assertThrows(
          InvalidAdherenceSourceException.class,
          () -> adapter.olderAdultIdsWithOutcomes(from, to, "adult-a", 2));
    }
    when(source.findOlderAdultIdsWithIntakes(from, to, null, 2)).thenReturn(null);
    assertThrows(
        InvalidAdherenceSourceException.class,
        () -> adapter.olderAdultIdsWithOutcomes(from, to, null, 2));
  }

  @Test
  void preservesOrderedImmutablePagesAndSourceFailureIdentity() {
    when(source.findOlderAdultIdsWithIntakes(from, to, null, 2))
        .thenReturn(List.of("adult-a", "adult-b"));
    var page = adapter.olderAdultIdsWithOutcomes(from, to, null, 2);
    assertEquals(List.of("adult-a", "adult-b"), page);
    assertThrows(UnsupportedOperationException.class, page::clear);
    var failure = new IllegalStateException("source unavailable");
    when(source.findOlderAdultIdsWithIntakes(from, to, "adult-b", 2)).thenThrow(failure);
    assertSame(
        failure,
        assertThrows(
                AdherenceSourceUnavailableException.class,
                () -> adapter.olderAdultIdsWithOutcomes(from, to, "adult-b", 2))
            .getCause());
  }
}
