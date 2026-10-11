package com.tata.intakeexecution.application.internal.commandservices;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.tata.intakeexecution.domain.model.aggregates.Intake;
import com.tata.intakeexecution.domain.model.commands.ReportUnconfirmedIntakesCommand;
import com.tata.intakeexecution.domain.model.valueobjects.ConfirmationChannel;
import com.tata.intakeexecution.domain.model.valueobjects.IntakeStatus;
import com.tata.intakeexecution.domain.model.valueobjects.MedicationSnapshot;
import com.tata.intakeexecution.domain.repositories.IntakeRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

class ReportUnconfirmedIntakesCommandHandlerTest {
  private final Instant scheduled = Instant.parse("2026-10-11T12:00:00Z");
  private final Instant now = scheduled.plusSeconds(600);
  private final IntakeRepository repository = mock(IntakeRepository.class);
  private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);

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
  void staleCandidateCannotOverwriteARecordedConfirmation() {
    var current = pending();
    current.confirm(now, ConfirmationChannel.TOUCH);
    when(repository.findUnreportedPendingDue(now)).thenReturn(List.of(pending()));
    when(repository.findByIdForConfirmation("intake")).thenReturn(Optional.of(current));
    assertEquals(
        0,
        new ReportUnconfirmedIntakesCommandHandler(repository, events)
            .handle(new ReportUnconfirmedIntakesCommand(now, now)));
    verify(repository, never()).saveAll(any());
    verifyNoInteractions(events);
    assertEquals(now, current.confirmedAt());
  }

  @Test
  void anotherSchedulerHavingReportedTheCandidateDoesNotPublishAgain() {
    var current = pending();
    current.reportUnconfirmed(now.minusSeconds(1));
    when(repository.findUnreportedPendingDue(now)).thenReturn(List.of(pending()));
    when(repository.findByIdForConfirmation("intake")).thenReturn(Optional.of(current));
    assertEquals(
        0,
        new ReportUnconfirmedIntakesCommandHandler(repository, events)
            .handle(new ReportUnconfirmedIntakesCommand(now, now)));
    verify(repository, never()).saveAll(any());
    verifyNoInteractions(events);
  }
}
