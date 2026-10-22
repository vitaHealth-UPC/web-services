package com.vitahealth.tata.intakeexecution.application.internal.commandservices;

import com.vitahealth.tata.intakeexecution.application.IntakeApplicationException;
import com.vitahealth.tata.intakeexecution.application.commandservices.ConfirmIntakeCommandService;
import com.vitahealth.tata.intakeexecution.application.internal.outboundservices.IntakeConfirmationAccessPort;
import com.vitahealth.tata.intakeexecution.application.result.IntakeResult;
import com.vitahealth.tata.intakeexecution.domain.model.commands.ConfirmIntakeCommand;
import com.vitahealth.tata.intakeexecution.domain.repositories.IntakeRepository;
import com.vitahealth.tata.shared.application.result.ApplicationError;
import com.vitahealth.tata.shared.application.result.Result;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Revalidates caller access after provider work while holding the intake mutation lock. */
@Service
public class AuthenticatedIntakeConfirmationService {
  private final IntakeRepository repository;
  private final IntakeConfirmationAccessPort access;
  private final ConfirmIntakeCommandService confirmation;

  /** Injects locking, current authorization and the existing atomic confirmation operation. */
  public AuthenticatedIntakeConfirmationService(
      IntakeRepository repository,
      IntakeConfirmationAccessPort access,
      ConfirmIntakeCommandService confirmation) {
    this.repository = repository;
    this.access = access;
    this.confirmation = confirmation;
  }

  /** Keeps the final access check and confirmation in one transaction, without provider calls. */
  @Transactional
  public Result<IntakeResult, ApplicationError> handle(
      ConfirmIntakeCommand command, String reference) {
    var intake =
        repository
            .findByIdForConfirmation(command.intakeId())
            .orElseThrow(
                () ->
                    new IntakeApplicationException(
                        IntakeApplicationException.Code.INTAKE_NOT_FOUND, "intake not found"));
    access.requireAuthorized(reference, intake.olderAdultId());
    return confirmation.handle(command);
  }
}
