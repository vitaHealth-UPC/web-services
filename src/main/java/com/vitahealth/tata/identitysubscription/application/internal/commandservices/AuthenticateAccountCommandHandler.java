package com.vitahealth.tata.identitysubscription.application.internal.commandservices;

import com.vitahealth.tata.identitysubscription.application.internal.outboundservices.hashing.PasswordHasher;
import com.vitahealth.tata.identitysubscription.application.internal.outboundservices.tokens.SessionTokenService;
import com.vitahealth.tata.identitysubscription.application.result.SessionResult;
import com.vitahealth.tata.identitysubscription.domain.model.commands.AuthenticateFamilyCommand;
import com.vitahealth.tata.identitysubscription.domain.model.valueobjects.EmailAddress;
import com.vitahealth.tata.identitysubscription.domain.repositories.AccountRepository;
import com.vitahealth.tata.shared.application.result.ApplicationError;
import com.vitahealth.tata.shared.application.result.Result;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Validates caregiver credentials and issues a revocable session. */
@Service
@Transactional
public class AuthenticateAccountCommandHandler {
  private final AccountRepository accountRepository;
  private final PasswordHasher passwordHasher;
  private final SessionTokenService sessionTokenService;

  /** Injects the dependencies required for this account operation. */
  public AuthenticateAccountCommandHandler(
      AccountRepository accountRepository,
      PasswordHasher passwordHasher,
      SessionTokenService sessionTokenService) {
    this.accountRepository = accountRepository;
    this.passwordHasher = passwordHasher;
    this.sessionTokenService = sessionTokenService;
  }

  /** Executes this command without exposing password or challenge hashes. */
  public Result<SessionResult, ApplicationError> handle(AuthenticateFamilyCommand command) {
    final EmailAddress email;
    try {
      email = new EmailAddress(command.email());
    } catch (IllegalArgumentException exception) {
      return Result.failure(new ApplicationError("VALIDATION_ERROR", "request validation failed"));
    }
    var candidate = accountRepository.findByEmail(email.value());
    if (candidate.isEmpty()) {
      return Result.failure(new ApplicationError("INVALID_CREDENTIALS", "invalid credentials"));
    }
    var account = candidate.get();
    if (!account.canAuthenticate()) {
      return Result.failure(new ApplicationError("ACCOUNT_NOT_ACTIVE", "account is not active"));
    }
    if (command.password() == null
        || !passwordHasher.matches(command.password(), account.passwordHash())) {
      return Result.failure(new ApplicationError("INVALID_CREDENTIALS", "invalid credentials"));
    }
    return Result.success(sessionTokenService.issue(account.id()));
  }
}
