package com.vitahealth.tata.identitysubscription.interfaces.rest.transform;

import com.vitahealth.tata.identitysubscription.application.IdentityApplicationException;
import com.vitahealth.tata.shared.application.result.ApplicationError;
import com.vitahealth.tata.shared.application.result.Result;
import com.vitahealth.tata.shared.infrastructure.i18n.ApplicationErrorMessageResolver;
import com.vitahealth.tata.shared.interfaces.rest.transform.ErrorResponseAssembler;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

/** Owns Identity's HTTP failure mapping while reusing the shared problem response envelope. */
@Component
public final class IdentityErrorResponseAssembler {
  private final ApplicationErrorMessageResolver messages;

  /** Injects shared message lookup while keeping Identity's code and HTTP status policy local. */
  public IdentityErrorResponseAssembler(ApplicationErrorMessageResolver messages) {
    this.messages = messages;
  }

  /** Localizes only public result failures at the HTTP boundary without changing successes. */
  public <T> Result<T, ApplicationError> localize(Result<T, ApplicationError> result) {
    return result.mapError(this::localizedError);
  }

  /** Preserves the stable public error code and message of an existing application failure. */
  public ResponseEntity<ProblemDetail> from(IdentityApplicationException exception) {
    return from(new ApplicationError(exception.code().name(), exception.getMessage()));
  }

  /** Translates an explicit application result failure without exposing internal details. */
  public ResponseEntity<ProblemDetail> from(ApplicationError error) {
    return ErrorResponseAssembler.toErrorResponseFromApplicationError(
        localizedError(error), status(error));
  }

  private ApplicationError localizedError(ApplicationError error) {
    return messages.resolve("identity.error", error, LocaleContextHolder.getLocale());
  }

  /** Maps known Identity codes explicitly; unknown technical failures remain server errors. */
  public static HttpStatus status(ApplicationError error) {
    return switch (error.code()) {
      case "DEVICE_REGISTRATION_CONFLICT", "DUPLICATE_EMAIL", "PIN_ALREADY_REGISTERED" ->
          HttpStatus.CONFLICT;
      case "ACCOUNT_NOT_FOUND", "PIN_NOT_FOUND", "PLAN_NOT_FOUND" -> HttpStatus.NOT_FOUND;
      case "INVALID_VERIFICATION", "INVALID_CREDENTIALS", "INVALID_PIN" -> HttpStatus.UNAUTHORIZED;
      case "VERIFICATION_EXPIRED" -> HttpStatus.GONE;
      case "DEVICE_ACCESS_DENIED", "ACCOUNT_NOT_ACTIVE", "PIN_LOCKED" -> HttpStatus.FORBIDDEN;
      case "SOCIAL_LINK_REQUIRED", "SOCIAL_LINK_CONFLICT" -> HttpStatus.CONFLICT;
      case "INVALID_SOCIAL_CREDENTIAL" -> HttpStatus.UNAUTHORIZED;
      case "SOCIAL_EMAIL_REQUIRED" -> HttpStatus.UNPROCESSABLE_CONTENT;
      case "SOCIAL_PROVIDER_UNAVAILABLE" -> HttpStatus.SERVICE_UNAVAILABLE;
      case "INVALID_RECOVERY_CODE" -> HttpStatus.BAD_REQUEST;
      case "VALIDATION_ERROR" -> HttpStatus.BAD_REQUEST;
      default -> HttpStatus.INTERNAL_SERVER_ERROR;
    };
  }
}
