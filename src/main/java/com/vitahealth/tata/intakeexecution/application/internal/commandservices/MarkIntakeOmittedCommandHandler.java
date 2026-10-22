package com.vitahealth.tata.intakeexecution.application.internal.commandservices;

import com.vitahealth.tata.intakeexecution.application.commandservices.MarkIntakeOmittedCommandService;
import com.vitahealth.tata.intakeexecution.domain.model.commands.MarkIntakeOmittedCommand;
import com.vitahealth.tata.intakeexecution.domain.repositories.IntakeRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Applies a definitive omission only while the locked source evidence remains pending. */
@Service
public class MarkIntakeOmittedCommandHandler implements MarkIntakeOmittedCommandService {

  private final IntakeRepository repository;

  /** Injects the source evidence repository with mandatory transactional lookups. */
  public MarkIntakeOmittedCommandHandler(IntakeRepository repository) {
    this.repository = repository;
  }

  /** Retains definitive confirmations and idempotent omission outcomes without another write. */
  @Override
  @Transactional
  public void handle(MarkIntakeOmittedCommand command) {
    repository
        .findByIdForConfirmation(command.intakeId())
        .ifPresent(
            intake -> {
              if (intake.markOmitted()) {
                repository.saveAll(List.of(intake));
              }
            });
  }
}
