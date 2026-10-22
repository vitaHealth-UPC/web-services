package com.vitahealth.tata.intakeexecution.application.internal.commandservices;

import com.vitahealth.tata.intakeexecution.application.IntakeApplicationException;
import com.vitahealth.tata.intakeexecution.application.commandservices.ConfirmIntakeCommandService;
import com.vitahealth.tata.intakeexecution.application.result.IntakeResult;
import com.vitahealth.tata.intakeexecution.domain.model.commands.ConfirmIntakeCommand;
import com.vitahealth.tata.shared.application.result.ApplicationError;
import com.vitahealth.tata.shared.application.result.Result;
import org.springframework.stereotype.Service;

/** Captures expected ConfirmIntake rejections after the handler's transaction completes. */
@Service
public class ConfirmIntakeCommandServiceImpl implements ConfirmIntakeCommandService {
  private final ConfirmIntakeCommandHandler handler;

  /** Injects the confirmation coordinator without opening an outer transaction. */
  public ConfirmIntakeCommandServiceImpl(ConfirmIntakeCommandHandler handler) {
    this.handler = handler;
  }

  /** Preserves technical exceptions for rollback and the server failure boundary. */
  @Override
  public Result<IntakeResult, ApplicationError> handle(ConfirmIntakeCommand command) {
    try {
      return Result.success(handler.handle(command));
    } catch (IntakeApplicationException rejection) {
      return Result.failure(rejection.toApplicationError());
    }
  }
}
