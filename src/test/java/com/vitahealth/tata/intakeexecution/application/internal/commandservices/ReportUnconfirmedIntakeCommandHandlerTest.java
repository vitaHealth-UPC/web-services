package com.vitahealth.tata.intakeexecution.application.internal.commandservices;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.vitahealth.tata.intakeexecution.application.internal.outboundservices.events.IntakeUnconfirmedEventPublisher;
import com.vitahealth.tata.intakeexecution.domain.model.aggregates.Intake;
import com.vitahealth.tata.intakeexecution.domain.model.commands.ReportUnconfirmedIntakeCommand;
import com.vitahealth.tata.intakeexecution.domain.model.valueobjects.ConfirmationChannel;
import com.vitahealth.tata.intakeexecution.domain.model.valueobjects.IntakeStatus;
import com.vitahealth.tata.intakeexecution.domain.model.valueobjects.MedicationSnapshot;
import com.vitahealth.tata.intakeexecution.domain.repositories.IntakeRepository;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ReportUnconfirmedIntakeCommandHandlerTest {
  private final Instant scheduled = Instant.parse("2026-10-11T12:00:00Z");
  private final Instant now = scheduled.plusSeconds(600);
  private final IntakeRepository repository = mock(IntakeRepository.class);
  private final IntakeUnconfirmedEventPublisher events =
      mock(IntakeUnconfirmedEventPublisher.class);

  private Intake pending() {
    return Intake.rehydrate(
        "intake",
        "treatment",
        "medication",
        "adult",
        new MedicationSnapshot("Losartan", "1 tableta", "Con agua"),
        scheduled,
        IntakeStatus.PENDING,
        scheduled.minusSeconds(60));
  }

  @Test
  void explicitTreatmentDelayUsesItsExactBoundaryRatherThanSystemCutoff() {
    for (int minutes : new int[] {0, 5, 25}) {
      var source = pending();
      var localRepository = mock(IntakeRepository.class);
      var localEvents = mock(IntakeUnconfirmedEventPublisher.class);
      when(localRepository.findByIdForConfirmation("intake")).thenReturn(Optional.of(source));
      var handler =
          new ReportUnconfirmedIntakeCommandHandler(
              localRepository,
              localEvents,
              (treatment, adult) -> {
                assertEquals("treatment", treatment);
                assertEquals("adult", adult);
                return Optional.of(minutes);
              });
      var boundary = scheduled.plusSeconds(minutes * 60L);
      assertEquals(
          false,
          handler.handle(
              new ReportUnconfirmedIntakeCommand(
                  "intake", scheduled.minusSeconds(600), boundary.minusSeconds(1))));
      verifyNoInteractions(localEvents);
      assertEquals(
          true,
          handler.handle(
              new ReportUnconfirmedIntakeCommand("intake", scheduled.minusSeconds(600), boundary)));
      verify(localEvents, times(1)).publish(any());
    }
  }

  @Test
  void staleCandidateCannotOverwriteARecordedConfirmation() {
    var current = pending();
    current.confirm(now, ConfirmationChannel.TOUCH);
    when(repository.findByIdForConfirmation("intake")).thenReturn(Optional.of(current));
    assertEquals(
        false,
        new ReportUnconfirmedIntakeCommandHandler(repository, events)
            .handle(new ReportUnconfirmedIntakeCommand("intake", now, now)));
    verify(repository, never()).saveAll(any());
    verifyNoInteractions(events);
    assertEquals(now, current.confirmedAt());
  }

  @Test
  void anotherSchedulerHavingReportedTheCandidateDoesNotPublishAgain() {
    var current = pending();
    current.reportUnconfirmed(now.minusSeconds(1));
    when(repository.findByIdForConfirmation("intake")).thenReturn(Optional.of(current));
    assertEquals(
        false,
        new ReportUnconfirmedIntakeCommandHandler(repository, events)
            .handle(new ReportUnconfirmedIntakeCommand("intake", now, now)));
    verify(repository, never()).saveAll(any());
    verifyNoInteractions(events);
  }
}
