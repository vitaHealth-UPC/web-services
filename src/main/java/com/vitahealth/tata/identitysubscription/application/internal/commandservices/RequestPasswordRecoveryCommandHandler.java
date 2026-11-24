package com.vitahealth.tata.identitysubscription.application.internal.commandservices;

import com.vitahealth.tata.identitysubscription.application.internal.outboundservices.*;
import com.vitahealth.tata.identitysubscription.application.internal.outboundservices.hashing.PasswordHasher;
import com.vitahealth.tata.identitysubscription.domain.model.commands.RequestPasswordRecoveryCommand;
import com.vitahealth.tata.identitysubscription.domain.model.valueobjects.*;
import com.vitahealth.tata.identitysubscription.domain.repositories.*;
import com.vitahealth.tata.shared.application.result.*;
import java.time.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Always accepts valid email syntax publicly, while issuing only for active accounts. */
@Service
@Transactional
public class RequestPasswordRecoveryCommandHandler {
  private final AccountRepository accounts;
  private final PasswordRecoveryRepository recoveries;
  private final VerificationCodeGenerator generator;
  private final PasswordHasher hasher;
  private final QueuePasswordRecoveryDeliveryPort delivery;
  private final Clock clock;

  public RequestPasswordRecoveryCommandHandler(
      AccountRepository accounts,
      PasswordRecoveryRepository recoveries,
      VerificationCodeGenerator generator,
      PasswordHasher hasher,
      QueuePasswordRecoveryDeliveryPort delivery,
      Clock clock) {
    this.accounts = accounts;
    this.recoveries = recoveries;
    this.generator = generator;
    this.hasher = hasher;
    this.delivery = delivery;
    this.clock = clock;
  }

  public Result<Boolean, ApplicationError> handle(RequestPasswordRecoveryCommand command) {
    final EmailAddress email;
    try {
      email = new EmailAddress(command.email());
    } catch (IllegalArgumentException error) {
      return Result.failure(new ApplicationError("VALIDATION_ERROR", "invalid email"));
    }
    // Perform the same credential work even when no eligible account exists.
    var code = new VerificationCode(generator.generate()).value();
    var hash = hasher.hash(code);
    var account = accounts.findByEmailForUpdate(email.value());
    if (account.isEmpty() || !account.get().canAuthenticate()) return Result.success(true);
    var now = clock.instant();
    var current = recoveries.findByAccountIdForUpdate(account.get().id());
    if (current.isPresent() && now.isBefore(current.get().issuedAt().plusSeconds(60)))
      return Result.success(true);
    var challenge = PasswordRecoveryChallenge.issue(hash, now, Duration.ofMinutes(10));
    recoveries.save(account.get().id(), challenge);
    delivery.queueRecovery(account.get().id(), email.value(), code, hash, challenge.expiresAt());
    return Result.success(true);
  }
}
