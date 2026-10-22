package com.vitahealth.tata.identitysubscription.application.internal.commandservices;

import com.vitahealth.tata.identitysubscription.application.internal.outboundservices.hashing.PasswordHasher;
import com.vitahealth.tata.identitysubscription.application.result.AccountResult;
import com.vitahealth.tata.identitysubscription.domain.model.commands.VerifyEmailCommand;
import com.vitahealth.tata.identitysubscription.domain.model.events.AccountEnabled;
import com.vitahealth.tata.identitysubscription.domain.model.valueobjects.EmailAddress;
import com.vitahealth.tata.identitysubscription.domain.model.valueobjects.VerificationCode;
import com.vitahealth.tata.identitysubscription.domain.repositories.AccountRepository;
import com.vitahealth.tata.shared.application.result.ApplicationError;
import com.vitahealth.tata.shared.application.result.Result;
import java.time.Clock;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Verifies the current challenge under an account lock and enables the account atomically. */
@Service
@Transactional
public class VerifyEmailCommandHandler {
  private final AccountRepository accounts;
  private final PasswordHasher passwordHasher;
  private final ApplicationEventPublisher events;
  private final Clock clock;

  /** Injects verification dependencies and the shared time source. */
  public VerifyEmailCommandHandler(
      AccountRepository accounts,
      PasswordHasher passwordHasher,
      ApplicationEventPublisher events,
      Clock clock) {
    this.accounts = accounts;
    this.passwordHasher = passwordHasher;
    this.events = events;
    this.clock = clock;
  }

  /**
   * Enables an account only when its current verification code is valid and unexpired.
   *
   * @param command email and challenge supplied by the account owner
   * @return the enabled account projection, or an expected failure without saving or publishing
   *     activation
   */
  public Result<AccountResult, ApplicationError> handle(VerifyEmailCommand command) {
    final EmailAddress email;
    try {
      email = new EmailAddress(command.email());
    } catch (IllegalArgumentException exception) {
      return Result.failure(new ApplicationError("VALIDATION_ERROR", "request validation failed"));
    }
    final VerificationCode code;
    try {
      code = new VerificationCode(command.code());
    } catch (IllegalArgumentException exception) {
      return Result.failure(
          new ApplicationError("INVALID_VERIFICATION", "verification code is invalid"));
    }
    var candidate = accounts.findByEmailForUpdate(email.value());
    if (candidate.isEmpty()) {
      return Result.failure(new ApplicationError("ACCOUNT_NOT_FOUND", "account not found"));
    }
    var account = candidate.get();
    var now = clock.instant();
    if (!account.canCompleteVerification(now)) {
      return Result.failure(
          new ApplicationError("VERIFICATION_EXPIRED", "verification code expired"));
    }
    if (!passwordHasher.matches(code.value(), account.verificationCodeHash())) {
      return Result.failure(
          new ApplicationError("INVALID_VERIFICATION", "verification code is invalid"));
    }
    account.completeVerification();
    var saved = accounts.save(account);
    events.publishEvent(new AccountEnabled(saved.id(), now));
    return Result.success(
        new AccountResult(saved.id(), saved.name(), saved.email().value(), saved.status()));
  }
}
