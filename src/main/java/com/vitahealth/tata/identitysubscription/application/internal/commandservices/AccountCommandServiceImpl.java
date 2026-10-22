package com.vitahealth.tata.identitysubscription.application.internal.commandservices;

import com.vitahealth.tata.identitysubscription.application.commandservices.AccountCommandService;
import com.vitahealth.tata.identitysubscription.application.internal.outboundservices.tokens.SessionTokenService;
import com.vitahealth.tata.identitysubscription.application.result.AccountResult;
import com.vitahealth.tata.identitysubscription.application.result.SessionResult;
import com.vitahealth.tata.identitysubscription.application.result.VerifiedAccountResult;
import com.vitahealth.tata.identitysubscription.domain.exceptions.DuplicateAccountEmailException;
import com.vitahealth.tata.identitysubscription.domain.model.commands.AuthenticateFamilyCommand;
import com.vitahealth.tata.identitysubscription.domain.model.commands.RegisterFamilyAccountCommand;
import com.vitahealth.tata.identitysubscription.domain.model.commands.RequestNewVerificationCommand;
import com.vitahealth.tata.identitysubscription.domain.model.commands.VerifyEmailCommand;
import com.vitahealth.tata.shared.application.result.ApplicationError;
import com.vitahealth.tata.shared.application.result.Result;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Coordinates account command handlers and the atomic verification/session workflow. */
@Service
public class AccountCommandServiceImpl implements AccountCommandService {
  private final RegisterAccountCommandHandler registerAccount;
  private final VerifyEmailCommandHandler verifyEmail;
  private final RequestVerificationCommandHandler requestVerification;
  private final AuthenticateAccountCommandHandler authenticateAccount;
  private final SessionTokenService sessionTokens;

  /** Injects handlers with independent responsibilities and the session issuance port. */
  public AccountCommandServiceImpl(
      RegisterAccountCommandHandler registerAccount,
      VerifyEmailCommandHandler verifyEmail,
      RequestVerificationCommandHandler requestVerification,
      AuthenticateAccountCommandHandler authenticateAccount,
      SessionTokenService sessionTokens) {
    this.registerAccount = registerAccount;
    this.verifyEmail = verifyEmail;
    this.requestVerification = requestVerification;
    this.authenticateAccount = authenticateAccount;
    this.sessionTokens = sessionTokens;
  }

  /** Delegates registration and translates only the rolled-back email uniqueness conflict. */
  @Override
  public Result<AccountResult, ApplicationError> register(RegisterFamilyAccountCommand command) {
    try {
      return registerAccount.handle(command);
    } catch (DuplicateAccountEmailException exception) {
      return Result.failure(new ApplicationError("DUPLICATE_EMAIL", "email already registered"));
    }
  }

  /** Delegates current challenge verification without issuing a credential. */
  @Override
  public Result<AccountResult, ApplicationError> verify(VerifyEmailCommand command) {
    return verifyEmail.handle(command);
  }

  /** Enables the account and creates its session in the same transaction. */
  @Override
  @Transactional
  public Result<VerifiedAccountResult, ApplicationError> verifyAndAuthenticate(
      VerifyEmailCommand command) {
    return verifyEmail
        .handle(command)
        .map(account -> new VerifiedAccountResult(account, sessionTokens.issue(account.id())));
  }

  /** Delegates challenge renewal and durable delivery intention creation. */
  @Override
  public Result<AccountResult, ApplicationError> requestNewVerification(
      RequestNewVerificationCommand command) {
    return requestVerification.handle(command);
  }

  /** Delegates caregiver credential authentication without exposing persisted hashes. */
  @Override
  public Result<SessionResult, ApplicationError> authenticate(AuthenticateFamilyCommand command) {
    return authenticateAccount.handle(command);
  }
}
