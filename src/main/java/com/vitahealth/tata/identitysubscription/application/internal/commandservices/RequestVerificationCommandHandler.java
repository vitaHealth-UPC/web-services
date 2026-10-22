package com.vitahealth.tata.identitysubscription.application.internal.commandservices;

import com.vitahealth.tata.identitysubscription.application.internal.outboundservices.QueueVerificationDeliveryPort;
import com.vitahealth.tata.identitysubscription.application.internal.outboundservices.VerificationCodeGenerator;
import com.vitahealth.tata.identitysubscription.application.internal.outboundservices.hashing.PasswordHasher;
import com.vitahealth.tata.identitysubscription.application.result.AccountResult;
import com.vitahealth.tata.identitysubscription.domain.model.aggregates.Account;
import com.vitahealth.tata.identitysubscription.domain.model.commands.RequestNewVerificationCommand;
import com.vitahealth.tata.identitysubscription.domain.model.valueobjects.AccountStatus;
import com.vitahealth.tata.identitysubscription.domain.model.valueobjects.EmailAddress;
import com.vitahealth.tata.identitysubscription.domain.model.valueobjects.VerificationCode;
import com.vitahealth.tata.identitysubscription.domain.repositories.AccountRepository;
import com.vitahealth.tata.shared.application.result.ApplicationError;
import com.vitahealth.tata.shared.application.result.Result;
import java.time.Clock;
import java.time.Duration;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Renews the pending account challenge under its account lock. */
@Service
@Transactional
public class RequestVerificationCommandHandler {
  private static final Duration VERIFICATION_TTL = Duration.ofMinutes(15);
  private final AccountRepository accountRepository;
  private final PasswordHasher passwordHasher;
  private final VerificationCodeGenerator verificationCodeGenerator;
  private final QueueVerificationDeliveryPort verificationDeliveryPort;
  private final Clock clock;

  /** Injects the dependencies required for this account operation. */
  public RequestVerificationCommandHandler(
      AccountRepository accountRepository,
      PasswordHasher passwordHasher,
      VerificationCodeGenerator verificationCodeGenerator,
      QueueVerificationDeliveryPort verificationDeliveryPort,
      Clock clock) {
    this.accountRepository = accountRepository;
    this.passwordHasher = passwordHasher;
    this.verificationCodeGenerator = verificationCodeGenerator;
    this.verificationDeliveryPort = verificationDeliveryPort;
    this.clock = clock;
  }

  /** Executes this command without exposing password or challenge hashes. */
  public Result<AccountResult, ApplicationError> handle(RequestNewVerificationCommand command) {
    final EmailAddress email;
    try {
      email = new EmailAddress(command.email());
    } catch (IllegalArgumentException exception) {
      return Result.failure(new ApplicationError("VALIDATION_ERROR", "request validation failed"));
    }
    var candidate = accountRepository.findByEmailForUpdate(email.value());
    if (candidate.isEmpty()) {
      return Result.failure(new ApplicationError("ACCOUNT_NOT_FOUND", "account not found"));
    }
    var account = candidate.get();
    if (account.status() != AccountStatus.PENDING_VERIFICATION) {
      return Result.failure(
          new ApplicationError("ACCOUNT_NOT_ACTIVE", "account is not pending verification"));
    }

    var verificationCode = new VerificationCode(verificationCodeGenerator.generate()).value();
    account.renewVerificationCode(
        passwordHasher.hash(verificationCode), clock.instant(), VERIFICATION_TTL);

    var saved = accountRepository.save(account);
    verificationDeliveryPort.queue(
        saved.id(),
        saved.email().value(),
        verificationCode,
        saved.verificationCodeHash(),
        saved.verificationExpiresAt());
    return Result.success(toResult(saved));
  }

  private static AccountResult toResult(Account account) {
    return new AccountResult(
        account.id(), account.name(), account.email().value(), account.status());
  }
}
