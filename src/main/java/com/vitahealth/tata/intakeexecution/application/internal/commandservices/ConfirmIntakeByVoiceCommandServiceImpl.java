package com.vitahealth.tata.intakeexecution.application.internal.commandservices;

import com.vitahealth.tata.intakeexecution.application.IntakeApplicationException;
import com.vitahealth.tata.intakeexecution.application.commandservices.ConfirmIntakeByVoiceCommandService;
import com.vitahealth.tata.intakeexecution.application.internal.outboundservices.IntakeConfirmationAccessPort;
import com.vitahealth.tata.intakeexecution.application.result.VoiceConfirmationResult;
import com.vitahealth.tata.intakeexecution.domain.model.commands.ConfirmIntakeByVoiceCommand;
import com.vitahealth.tata.shared.application.result.ApplicationError;
import com.vitahealth.tata.shared.application.result.Result;
import org.springframework.stereotype.Service;

/** Captures expected ConfirmIntakeByVoice rejections after the handler's transaction completes. */
@Service
public class ConfirmIntakeByVoiceCommandServiceImpl implements ConfirmIntakeByVoiceCommandService {
  private final ConfirmIntakeByVoiceCommandHandler handler;
  private final IntakeConfirmationAccessPort access;

  /** Injects the confirmation coordinator without opening an outer transaction. */
  public ConfirmIntakeByVoiceCommandServiceImpl(
      ConfirmIntakeByVoiceCommandHandler handler, IntakeConfirmationAccessPort access) {
    this.handler = handler;
    this.access = access;
  }

  /** Preserves technical exceptions for rollback and the server failure boundary. */
  @Override
  public Result<VoiceConfirmationResult, ApplicationError> handle(
      ConfirmIntakeByVoiceCommand command) {
    try {
      return Result.success(handler.handle(command));
    } catch (IntakeApplicationException rejection) {
      return Result.failure(rejection.toApplicationError());
    }
  }

  /** Exchanges the bearer credential for an internal reference before recognizing audio. */
  @Override
  public Result<VoiceConfirmationResult, ApplicationError> handleAuthenticated(
      ConfirmIntakeByVoiceCommand command, String credential) {
    var reference = access.authenticate(credential);
    try {
      return Result.success(handler.handleAuthenticated(command, reference));
    } catch (IntakeApplicationException rejection) {
      return Result.failure(rejection.toApplicationError());
    }
  }
}
