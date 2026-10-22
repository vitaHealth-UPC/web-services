package com.vitahealth.tata.identitysubscription.application.internal.commandservices;

import static com.vitahealth.tata.shared.domain.validation.DomainText.requireText;

import com.vitahealth.tata.identitysubscription.application.exceptions.UnsupportedCredentialException;
import com.vitahealth.tata.identitysubscription.application.internal.outboundservices.QueueVerificationDeliveryPort;
import com.vitahealth.tata.identitysubscription.application.internal.outboundservices.VerificationCodeGenerator;
import com.vitahealth.tata.identitysubscription.application.internal.outboundservices.hashing.PasswordHasher;
import com.vitahealth.tata.identitysubscription.application.result.AccountResult;
import com.vitahealth.tata.identitysubscription.domain.model.aggregates.Account;
import com.vitahealth.tata.identitysubscription.domain.model.commands.RegisterFamilyAccountCommand;
import com.vitahealth.tata.identitysubscription.domain.model.valueobjects.EmailAddress;
import com.vitahealth.tata.identitysubscription.domain.model.valueobjects.VerificationCode;
import com.vitahealth.tata.identitysubscription.domain.repositories.AccountRepository;
import com.vitahealth.tata.identitysubscription.domain.services.PasswordPolicy;
import com.vitahealth.tata.shared.application.result.ApplicationError;
import com.vitahealth.tata.shared.application.result.Result;
import java.time.Clock;
import java.time.Duration;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Registers an account with its initial verification challenge. */
@Service
@Transactional
public class RegisterAccountCommandHandler {
  private static final Duration VERIFICATION_TTL = Duration.ofMinutes(15);
  private final AccountRepository accountRepository;
  private final PasswordHasher passwordHasher;
  private final VerificationCodeGenerator verificationCodeGenerator;
  private final QueueVerificationDeliveryPort verificationDeliveryPort;
  private final Clock clock;

  /** Injects the dependencies required for this account operation. */
  public RegisterAccountCommandHandler(
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
  public Result<AccountResult, ApplicationError> handle(RegisterFamilyAccountCommand command) {
    final EmailAddress email;
    final String name;
    try {
      name = requireText(command.name(), "name");
      if (name.length() > 120) {
        throw new IllegalArgumentException("name exceeds supported length");
      }
      email = new EmailAddress(command.email());
      PasswordPolicy.requireValidRegistrationPassword(command.password());
    } catch (IllegalArgumentException exception) {
      return Result.failure(new ApplicationError("VALIDATION_ERROR", "request validation failed"));
    }
    if (accountRepository.findByEmail(email.value()).isPresent()) {
      return Result.failure(new ApplicationError("DUPLICATE_EMAIL", "email already registered"));
    }

    final String passwordHash;
    try {
      passwordHash = passwordHasher.hash(command.password());
    } catch (UnsupportedCredentialException exception) {
      return Result.failure(new ApplicationError("VALIDATION_ERROR", "request validation failed"));
    }

    var verificationCode = new VerificationCode(verificationCodeGenerator.generate()).value();
    var account =
        Account.register(
            name,
            email,
            passwordHash,
            passwordHasher.hash(verificationCode),
            clock.instant(),
            VERIFICATION_TTL);

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
