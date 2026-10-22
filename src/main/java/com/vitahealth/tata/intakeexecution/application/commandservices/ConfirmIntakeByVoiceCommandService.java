package com.vitahealth.tata.intakeexecution.application.commandservices;

import com.vitahealth.tata.intakeexecution.application.result.VoiceConfirmationResult;
import com.vitahealth.tata.intakeexecution.domain.model.commands.ConfirmIntakeByVoiceCommand;
import com.vitahealth.tata.shared.application.result.ApplicationError;
import com.vitahealth.tata.shared.application.result.Result;

/**
 * Public ConfirmIntakeByVoice outcomes, with expected rejections separated from technical failures.
 */
public interface ConfirmIntakeByVoiceCommandService {
  /**
   * Executes trusted in-process confirmation while retaining definitive evidence on idempotent
   * retries.
   */
  Result<VoiceConfirmationResult, ApplicationError> handle(ConfirmIntakeByVoiceCommand command);

  /** Authenticates an HTTP caller and revalidates its reference after provider work. */
  Result<VoiceConfirmationResult, ApplicationError> handleAuthenticated(
      ConfirmIntakeByVoiceCommand command, String credential);
}
