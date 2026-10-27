package com.vitahealth.tata.identitysubscription.application.commandservices;

import com.vitahealth.tata.identitysubscription.domain.model.commands.*;
import com.vitahealth.tata.shared.application.result.*;

public interface PasswordRecoveryCommandService {
  Result<Boolean, ApplicationError> request(RequestPasswordRecoveryCommand command);

  Result<Boolean, ApplicationError> complete(CompletePasswordRecoveryCommand command);
}
