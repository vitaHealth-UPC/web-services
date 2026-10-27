package com.vitahealth.tata.identitysubscription.application.internal.commandservices;

import com.vitahealth.tata.identitysubscription.application.internal.outboundservices.RevokeAccountSessionsPort;
import com.vitahealth.tata.identitysubscription.application.internal.outboundservices.hashing.PasswordHasher;
import com.vitahealth.tata.identitysubscription.domain.model.commands.CompletePasswordRecoveryCommand;
import com.vitahealth.tata.identitysubscription.domain.model.valueobjects.*;
import com.vitahealth.tata.identitysubscription.domain.repositories.*;
import com.vitahealth.tata.identitysubscription.domain.services.PasswordPolicy;
import com.vitahealth.tata.shared.application.result.*;
import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Atomically changes encoded credentials, consumes recovery evidence and revokes account sessions.
 */
@Service
@Transactional
public class CompletePasswordRecoveryCommandHandler {
  private final AccountRepository accounts;
  private final PasswordRecoveryRepository recoveries;
  private final PasswordHasher hasher;
  private final RevokeAccountSessionsPort sessions;
  private final Clock clock;
  private final String unavailableChallengeHash;

  public CompletePasswordRecoveryCommandHandler(
      AccountRepository accounts,
      PasswordRecoveryRepository recoveries,
      PasswordHasher hasher,
      RevokeAccountSessionsPort sessions,
      Clock clock) {
    this.accounts = accounts;
    this.recoveries = recoveries;
    this.hasher = hasher;
    this.sessions = sessions;
    this.clock = clock;
    this.unavailableChallengeHash = hasher.hash("000000");
  }

  public Result<Boolean, ApplicationError> handle(CompletePasswordRecoveryCommand command) {
    final EmailAddress email;
    final String code;
    try {
      email = new EmailAddress(command.email());
      code = new VerificationCode(command.code()).value();
      PasswordPolicy.requireValidRegistrationPassword(command.newPassword());
    } catch (IllegalArgumentException error) {
      return Result.failure(new ApplicationError("VALIDATION_ERROR", "recovery input is invalid"));
    }
    var account = accounts.findByEmailForUpdate(email.value());
    var recovery =
        account
            .filter(value -> value.canAuthenticate())
            .flatMap(value -> recoveries.findByAccountIdForUpdate(value.id()));
    var matches =
        hasher.matches(
            code,
            recovery.map(PasswordRecoveryChallenge::codeHash).orElse(unavailableChallengeHash));
    if (recovery.isEmpty()) return invalidCode();
    var attempt = recovery.get().attempt(matches, clock.instant());
    if (attempt.outcome() != PasswordRecoveryChallenge.Outcome.ACCEPTED) {
      recoveries.save(account.get().id(), attempt.challenge());
      return invalidCode();
    }
    final String encoded;
    try {
      encoded = hasher.hash(command.newPassword());
    } catch (IllegalArgumentException error) {
      return Result.failure(new ApplicationError("VALIDATION_ERROR", "password cannot be encoded"));
    }
    account.get().replacePasswordHash(encoded);
    accounts.save(account.get());
    recoveries.save(account.get().id(), attempt.challenge());
    sessions.revokeCaregiverSessions(account.get().id());
    return Result.success(true);
  }

  private static Result<Boolean, ApplicationError> invalidCode() {
    return Result.failure(
        new ApplicationError("INVALID_RECOVERY_CODE", "recovery evidence is invalid"));
  }
}
