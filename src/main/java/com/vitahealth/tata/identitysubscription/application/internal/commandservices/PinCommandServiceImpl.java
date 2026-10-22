package com.vitahealth.tata.identitysubscription.application.internal.commandservices;

import com.vitahealth.tata.identitysubscription.application.IdentityApplicationException;
import com.vitahealth.tata.identitysubscription.application.commandservices.PinCommandService;
import com.vitahealth.tata.identitysubscription.application.result.PinEnrollmentResult;
import com.vitahealth.tata.identitysubscription.application.result.SessionResult;
import com.vitahealth.tata.identitysubscription.domain.exceptions.DuplicatePinCredentialException;
import com.vitahealth.tata.identitysubscription.domain.model.commands.AuthenticateWithPinCommand;
import com.vitahealth.tata.identitysubscription.domain.model.commands.RegisterPinCommand;
import com.vitahealth.tata.shared.application.result.ApplicationError;
import com.vitahealth.tata.shared.application.result.Result;
import org.springframework.stereotype.Service;

/** Delegates PIN enrollment and access to their transaction-owning handlers. */
@Service
public class PinCommandServiceImpl implements PinCommandService {
  private final RegisterPinCommandHandler registration;
  private final AuthenticatePinCommandHandler authentication;

  /** Injects independently scoped PIN command handlers. */
  public PinCommandServiceImpl(
      RegisterPinCommandHandler registration, AuthenticatePinCommandHandler authentication) {
    this.registration = registration;
    this.authentication = authentication;
  }

  /**
   * Delegates enrollment and translates the conflict only after its handler transaction rolls back.
   */
  @Override
  public Result<PinEnrollmentResult, ApplicationError> register(RegisterPinCommand command) {
    try {
      return registration.handle(command);
    } catch (DuplicatePinCredentialException exception) {
      return Result.failure(
          new ApplicationError("PIN_ALREADY_REGISTERED", "PIN already registered"));
    }
  }

  /**
   * Maps expected rejections after the authentication handler has persisted its failure counter.
   */
  @Override
  public Result<SessionResult, ApplicationError> authenticate(AuthenticateWithPinCommand command) {
    try {
      return Result.success(authentication.handle(command));
    } catch (IdentityApplicationException exception) {
      return Result.failure(new ApplicationError(exception.code().name(), exception.getMessage()));
    } catch (IllegalArgumentException exception) {
      return Result.failure(new ApplicationError("VALIDATION_ERROR", "request validation failed"));
    }
  }
}
