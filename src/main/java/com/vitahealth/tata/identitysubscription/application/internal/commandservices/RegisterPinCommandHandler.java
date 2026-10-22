package com.vitahealth.tata.identitysubscription.application.internal.commandservices;

import com.vitahealth.tata.identitysubscription.application.internal.outboundservices.hashing.PasswordHasher;
import com.vitahealth.tata.identitysubscription.application.result.PinEnrollmentResult;
import com.vitahealth.tata.identitysubscription.domain.model.aggregates.PinCredential;
import com.vitahealth.tata.identitysubscription.domain.model.commands.RegisterPinCommand;
import com.vitahealth.tata.identitysubscription.domain.model.valueobjects.PinCode;
import com.vitahealth.tata.identitysubscription.domain.repositories.PinCredentialRepository;
import com.vitahealth.tata.shared.application.result.ApplicationError;
import com.vitahealth.tata.shared.application.result.Result;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Enrolls one encoded PIN for an adult with explicit transactional ownership. */
@Service
@Transactional
public class RegisterPinCommandHandler {
  private final PinCredentialRepository repository;
  private final PasswordHasher passwordHasher;

  /** Injects dependencies for this PIN operation. */
  public RegisterPinCommandHandler(
      PinCredentialRepository repository, PasswordHasher passwordHasher) {
    this.repository = repository;
    this.passwordHasher = passwordHasher;
  }

  /**
   * Enrolls a previously absent credential and returns validation or duplicate enrollment failures.
   */
  public Result<PinEnrollmentResult, ApplicationError> handle(RegisterPinCommand command) {
    try {
      new PinCode(command.pin());
    } catch (IllegalArgumentException exception) {
      return Result.failure(new ApplicationError("VALIDATION_ERROR", "request validation failed"));
    }
    if (repository.findByOlderAdultId(command.olderAdultId()).isPresent()) {
      return Result.failure(
          new ApplicationError("PIN_ALREADY_REGISTERED", "PIN already registered"));
    }
    repository.save(
        PinCredential.register(command.olderAdultId(), passwordHasher.hash(command.pin())));
    return Result.success(new PinEnrollmentResult(command.olderAdultId()));
  }
}
