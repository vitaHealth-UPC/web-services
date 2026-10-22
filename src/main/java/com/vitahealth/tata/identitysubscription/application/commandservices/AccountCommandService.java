package com.vitahealth.tata.identitysubscription.application.commandservices;

import com.vitahealth.tata.identitysubscription.application.result.AccountResult;
import com.vitahealth.tata.identitysubscription.application.result.SessionResult;
import com.vitahealth.tata.identitysubscription.application.result.VerifiedAccountResult;
import com.vitahealth.tata.identitysubscription.domain.model.commands.AuthenticateFamilyCommand;
import com.vitahealth.tata.identitysubscription.domain.model.commands.RegisterFamilyAccountCommand;
import com.vitahealth.tata.identitysubscription.domain.model.commands.RequestNewVerificationCommand;
import com.vitahealth.tata.identitysubscription.domain.model.commands.VerifyEmailCommand;
import com.vitahealth.tata.shared.application.result.ApplicationError;
import com.vitahealth.tata.shared.application.result.Result;

/** Account lifecycle commands; projections exclude stored password and challenge hashes. */
public interface AccountCommandService {
  /** Registers a pending account and creates its initial verification challenge. */
  Result<AccountResult, ApplicationError> register(RegisterFamilyAccountCommand command);

  /** Enables an account after validating its current, unexpired verification challenge. */
  Result<AccountResult, ApplicationError> verify(VerifyEmailCommand command);

  /** Enables the account and issues its session atomically. */
  Result<VerifiedAccountResult, ApplicationError> verifyAndAuthenticate(VerifyEmailCommand command);

  /** Replaces the challenge of an account that remains pending verification. */
  Result<AccountResult, ApplicationError> requestNewVerification(
      RequestNewVerificationCommand command);

  /** Issues a session only for valid credentials of an active account. */
  Result<SessionResult, ApplicationError> authenticate(AuthenticateFamilyCommand command);
}
