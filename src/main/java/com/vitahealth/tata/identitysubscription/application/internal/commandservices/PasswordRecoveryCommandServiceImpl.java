package com.vitahealth.tata.identitysubscription.application.internal.commandservices;

import com.vitahealth.tata.identitysubscription.application.commandservices.PasswordRecoveryCommandService;
import com.vitahealth.tata.identitysubscription.domain.model.commands.*;
import com.vitahealth.tata.shared.application.result.*;
import org.springframework.stereotype.Service;

@Service
public class PasswordRecoveryCommandServiceImpl implements PasswordRecoveryCommandService {
  private final RequestPasswordRecoveryCommandHandler requests;
  private final CompletePasswordRecoveryCommandHandler completions;

  public PasswordRecoveryCommandServiceImpl(
      RequestPasswordRecoveryCommandHandler requests,
      CompletePasswordRecoveryCommandHandler completions) {
    this.requests = requests;
    this.completions = completions;
  }

  @Override
  public Result<Boolean, ApplicationError> request(RequestPasswordRecoveryCommand command) {
    return requests.handle(command);
  }

  @Override
  public Result<Boolean, ApplicationError> complete(CompletePasswordRecoveryCommand command) {
    return completions.handle(command);
  }
}
