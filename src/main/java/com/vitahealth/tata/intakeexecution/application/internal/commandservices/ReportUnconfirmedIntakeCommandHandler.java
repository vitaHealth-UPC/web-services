package com.vitahealth.tata.intakeexecution.application.internal.commandservices;

import com.vitahealth.tata.intakeexecution.application.internal.outboundservices.events.IntakeUnconfirmedEventPublisher;
import com.vitahealth.tata.intakeexecution.domain.model.commands.ReportUnconfirmedIntakeCommand;
import com.vitahealth.tata.intakeexecution.domain.model.events.IntakeUnconfirmed;
import com.vitahealth.tata.intakeexecution.domain.repositories.IntakeRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Commits one source marker and its synchronous handoff independently of other candidates. */
@Service
public class ReportUnconfirmedIntakeCommandHandler {
  private final IntakeRepository repository;
  private final IntakeUnconfirmedEventPublisher events;

  /** Injects locked source persistence and typed event delivery. */
  public ReportUnconfirmedIntakeCommandHandler(
      IntakeRepository repository, IntakeUnconfirmedEventPublisher events) {
    this.repository = repository;
    this.events = events;
  }

  /** Rechecks locked evidence and rolls back both source and consumers when the handoff fails. */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public boolean handle(ReportUnconfirmedIntakeCommand command) {
    var intake = repository.findByIdForConfirmation(command.intakeId()).orElse(null);
    if (intake == null
        || intake.scheduledAt().isAfter(command.cutoff())
        || !intake.reportUnconfirmed(command.reportedAt())) return false;
    repository.saveAll(List.of(intake));
    events.publish(
        new IntakeUnconfirmed(
            intake.id(), intake.olderAdultId(), intake.medication().name(), intake.scheduledAt()));
    return true;
  }
}
