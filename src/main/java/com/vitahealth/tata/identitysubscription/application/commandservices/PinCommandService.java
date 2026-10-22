package com.vitahealth.tata.identitysubscription.application.commandservices;

import com.vitahealth.tata.identitysubscription.application.result.PinEnrollmentResult;
import com.vitahealth.tata.identitysubscription.application.result.SessionResult;
import com.vitahealth.tata.identitysubscription.domain.model.commands.AuthenticateWithPinCommand;
import com.vitahealth.tata.identitysubscription.domain.model.commands.RegisterPinCommand;
import com.vitahealth.tata.shared.application.result.ApplicationError;
import com.vitahealth.tata.shared.application.result.Result;

/** Adult PIN enrollment and authentication contracts; plaintext PINs never appear in results. */
public interface PinCommandService {
  /** Enrolls a PIN once; existing credentials must not be silently replaced. */
  Result<PinEnrollmentResult, ApplicationError> register(RegisterPinCommand command);

  /** Validates under lock and retains failure counters even when access is rejected. */
  Result<SessionResult, ApplicationError> authenticate(AuthenticateWithPinCommand command);
}
