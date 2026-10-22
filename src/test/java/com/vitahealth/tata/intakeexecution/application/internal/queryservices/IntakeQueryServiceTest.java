package com.vitahealth.tata.intakeexecution.application.internal.queryservices;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vitahealth.tata.intakeexecution.domain.model.aggregates.Intake;
import com.vitahealth.tata.intakeexecution.domain.model.queries.GetIntakeAgendaQuery;
import com.vitahealth.tata.intakeexecution.domain.model.queries.GetIntakeDetailQuery;
import com.vitahealth.tata.intakeexecution.domain.model.queries.GetNextIntakeQuery;
import com.vitahealth.tata.intakeexecution.domain.model.valueobjects.IntakeStatus;
import com.vitahealth.tata.intakeexecution.domain.model.valueobjects.MedicationSnapshot;
import com.vitahealth.tata.intakeexecution.domain.repositories.IntakeRepository;
import com.vitahealth.tata.shared.application.result.ApplicationError;
import com.vitahealth.tata.shared.application.result.Result;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;

/** Verifies public query outcomes distinguish valid absence, rejection and technical failure. */
class IntakeQueryServiceTest {
  private static final Instant NOW = Instant.parse("2026-10-15T08:00:00Z");
  private final IntakeRepository repository = mock(IntakeRepository.class);
  private final IntakeQueryServiceImpl service =
      new IntakeQueryServiceImpl(
          new GetIntakeAgendaQueryHandler(repository),
          new GetIntakeDetailQueryHandler(repository),
          new GetNextIntakeQueryHandler(repository, Clock.fixed(NOW, ZoneOffset.UTC)));

  @Test
  void validEmptySelectionsAreSuccessful() {
    when(repository.findAgenda("adult", NOW, NOW.plus(1, ChronoUnit.DAYS))).thenReturn(List.of());
    when(repository.findById("missing")).thenReturn(Optional.empty());
    when(repository.findNextPendingByOlderAdultId("adult", NOW)).thenReturn(Optional.empty());
    assertEquals(
        List.of(),
        success(
            service.handle(new GetIntakeAgendaQuery("adult", NOW, NOW.plus(1, ChronoUnit.DAYS)))));
    assertEquals(Optional.empty(), success(service.handle(new GetIntakeDetailQuery("missing"))));
    assertEquals(Optional.empty(), success(service.handle(new GetNextIntakeQuery("adult"))));
  }

  @Test
  void invalidReferencesAndRangesProduceExplicitRejectionsBeforeReading() {
    var outcomes =
        List.of(
            service.handle(new GetIntakeDetailQuery(" ")),
            service.handle(new GetNextIntakeQuery("")),
            service.handle(new GetIntakeAgendaQuery("adult", NOW, NOW)),
            service.handle(new GetIntakeAgendaQuery("adult", NOW, NOW.plus(9, ChronoUnit.DAYS))));
    for (var outcome : outcomes) {
      var failure = assertInstanceOf(Result.Failure.class, outcome);
      assertEquals("VALIDATION_ERROR", ((ApplicationError) failure.error()).code());
    }
    verifyNoInteractions(repository);
  }

  @Test
  void persistenceFailuresAreNotReportedAsAbsentOrInvalidInput() {
    when(repository.findById("failure"))
        .thenThrow(new DataAccessResourceFailureException("storage unavailable"));
    assertThrows(
        DataAccessResourceFailureException.class,
        () -> service.handle(new GetIntakeDetailQuery("failure")));
  }

  @Test
  void successfulEvidenceRetainsIdentityAndDoesNotClaimAConfirmationReplay() {
    var intake =
        Intake.rehydrate(
            "intake",
            "treatment",
            "medication",
            "adult",
            new MedicationSnapshot("Losartán", "1 comprimido", "Con agua"),
            NOW,
            IntakeStatus.PENDING,
            NOW.minusSeconds(60));
    when(repository.findById("intake")).thenReturn(Optional.of(intake));
    var result = success(service.handle(new GetIntakeDetailQuery(" intake "))).orElseThrow();
    assertEquals("intake", result.id());
    assertEquals(NOW, result.scheduledAt());
    assertEquals(IntakeStatus.PENDING, result.status());
    assertFalse(result.alreadyConfirmed());
  }

  private static <T> T success(Result<T, ApplicationError> outcome) {
    return switch (outcome) {
      case Result.Success<T, ApplicationError> value -> value.value();
      case Result.Failure<T, ApplicationError> failure -> throw new AssertionError(failure.error());
    };
  }
}
