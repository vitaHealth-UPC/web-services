package com.vitahealth.tata.intakeexecution.application.commandservices;

import com.vitahealth.tata.intakeexecution.application.result.IntakeResult;
import com.vitahealth.tata.intakeexecution.domain.model.commands.ConfirmIntakeCommand;
import com.vitahealth.tata.shared.application.result.ApplicationError;
import com.vitahealth.tata.shared.application.result.Result;

/** Public ConfirmIntake outcomes, with expected rejections separated from technical failures. */
public interface ConfirmIntakeCommandService {
  /** Executes confirmation while retaining definitive evidence on idempotent retries. */
  Result<IntakeResult, ApplicationError> handle(ConfirmIntakeCommand command);
}
