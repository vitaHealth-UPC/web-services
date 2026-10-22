package com.vitahealth.tata.identitysubscription.interfaces.rest;

import com.vitahealth.tata.identitysubscription.application.IdentityApplicationException;
import com.vitahealth.tata.identitysubscription.interfaces.rest.transform.IdentityErrorResponseAssembler;
import com.vitahealth.tata.shared.application.result.ApplicationError;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Translates legacy thrown failures using the same mapping as explicit application results. */
@RestControllerAdvice(
    assignableTypes = {
      PushDevicesController.class,
      AccountsController.class,
      EmailVerificationRequestsController.class,
      SessionsController.class,
      PinCredentialsController.class,
      PinSessionsController.class,
      SubscriptionsController.class
    })
public class IdentityExceptionHandler {
  private final IdentityErrorResponseAssembler errors;

  /** Injects the same localized response mapping used by explicit result failures. */
  public IdentityExceptionHandler(IdentityErrorResponseAssembler errors) {
    this.errors = errors;
  }

  /** Maps a thrown expected Identity failure to its stable public code and localized detail. */
  @ExceptionHandler(IdentityApplicationException.class)
  public ResponseEntity<ProblemDetail> handleIdentity(IdentityApplicationException exception) {
    return errors.from(exception);
  }

  /** Returns a localized validation response without exposing rejected credentials. */
  @ExceptionHandler({
    IllegalArgumentException.class,
    MethodArgumentNotValidException.class,
    HttpMessageNotReadableException.class
  })
  public ResponseEntity<ProblemDetail> handleValidation(Exception exception) {
    return errors.from(new ApplicationError("VALIDATION_ERROR", "request validation failed"));
  }
}
