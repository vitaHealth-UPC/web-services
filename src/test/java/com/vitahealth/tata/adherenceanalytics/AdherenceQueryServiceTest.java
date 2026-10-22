package com.vitahealth.tata.adherenceanalytics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.vitahealth.tata.adherenceanalytics.application.internal.outboundservices.IntakeOutcomePort;
import com.vitahealth.tata.adherenceanalytics.application.internal.queryservices.AdherenceQueryServiceImpl;
import com.vitahealth.tata.adherenceanalytics.application.queryservices.AdherenceQueryService;
import com.vitahealth.tata.adherenceanalytics.domain.model.queries.GetAdherencePeriodQuery;
import com.vitahealth.tata.adherenceanalytics.domain.model.valueobjects.IntakeOutcome;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Verifies adherence denominator rules and complete source outcome evidence. */
class AdherenceQueryServiceTest {
  private final Instant from = Instant.parse("2026-10-05T00:00:00Z");

  private AdherenceQueryService service(IntakeOutcome.Status... statuses) {
    var outcomes = mock(IntakeOutcomePort.class);
    when(outcomes.find(anyString(), any(Instant.class), any(Instant.class)))
        .thenReturn(
            Arrays.stream(statuses)
                .map(status -> new IntakeOutcome("medication", from, status))
                .toList());
    return new AdherenceQueryServiceImpl(outcomes, 3);
  }

  @Test
  void timelyAndLateConfirmationsProduceFullAdherence() {
    var metrics =
        service(IntakeOutcome.Status.CONFIRMED, IntakeOutcome.Status.LATE)
            .weekly(new GetAdherencePeriodQuery("owner", from, from.plusSeconds(86400)));
    assertEquals(100d, metrics.percentage());
    assertEquals(2, metrics.confirmedIntakes());
    assertEquals(1, metrics.onTimeIntakes());
    assertEquals(1, metrics.lateIntakes());
    assertEquals(0, metrics.omittedIntakes());
  }

  @Test
  void omittedOnlyProducesZeroAndPendingOnlyHasInsufficientEvidence() {
    var omitted =
        service(IntakeOutcome.Status.OMITTED)
            .weekly(new GetAdherencePeriodQuery("owner", from, from.plusSeconds(86400)));
    assertEquals(0d, omitted.percentage());
    assertEquals(1, omitted.totalIntakes());
    var pending =
        service(IntakeOutcome.Status.PENDING)
            .weekly(new GetAdherencePeriodQuery("owner", from, from.plusSeconds(86400)));
    assertEquals(0, pending.totalIntakes());
    assertEquals(
        List.of(),
        service(IntakeOutcome.Status.PENDING)
            .recommendations(
                new GetAdherencePeriodQuery("owner", from, from.plusSeconds(86400), "UTC")));
  }

  @Test
  void outcomeEvidenceRequiresMedicationScheduleAndStatus() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new IntakeOutcome(" ", from, IntakeOutcome.Status.PENDING));
    assertThrows(
        NullPointerException.class,
        () -> new IntakeOutcome("medication", null, IntakeOutcome.Status.PENDING));
    assertThrows(NullPointerException.class, () -> new IntakeOutcome("medication", from, null));
  }
}
