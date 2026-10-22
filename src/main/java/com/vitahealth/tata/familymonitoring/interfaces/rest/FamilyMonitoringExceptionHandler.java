package com.vitahealth.tata.familymonitoring.interfaces.rest;

import com.vitahealth.tata.familymonitoring.application.CareRelationshipRequiredException;
import com.vitahealth.tata.familymonitoring.application.exceptions.InvalidMonitoringSourceException;
import com.vitahealth.tata.familymonitoring.domain.exceptions.AlertNotFoundException;
import com.vitahealth.tata.familymonitoring.domain.exceptions.ContactChannelNotAvailableException;
import com.vitahealth.tata.familymonitoring.domain.exceptions.FamilyMonitorNotFoundException;
import com.vitahealth.tata.familymonitoring.domain.exceptions.InvalidAlertTransitionException;
import com.vitahealth.tata.familymonitoring.interfaces.rest.resources.ErrorResource;
import java.util.Locale;
import org.springframework.context.MessageSource;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Turns Family Monitoring failures into stable error codes. */
@RestControllerAdvice(basePackages = "com.vitahealth.tata.familymonitoring")
public class FamilyMonitoringExceptionHandler {
  private final MessageSource messages;

  /** Injects the shared resource catalog while retaining context-owned error codes. */
  public FamilyMonitoringExceptionHandler(MessageSource messages) {
    this.messages = messages;
  }

  /** Reports an obsolete write snapshot without exposing persistence diagnostics. */
  @ExceptionHandler(OptimisticLockingFailureException.class)
  public ResponseEntity<ErrorResource> handleConcurrentUpdate(
      OptimisticLockingFailureException exception, Locale locale) {
    return error(HttpStatus.CONFLICT, "CONCURRENT_UPDATE", locale);
  }

  /**
   * Keeps invalid source evidence distinct from malformed client requests and business conflicts.
   */
  @ExceptionHandler(InvalidMonitoringSourceException.class)
  public ResponseEntity<ErrorResource> handleInvalidSource(
      InvalidMonitoringSourceException exception, Locale locale) {
    return error(HttpStatus.INTERNAL_SERVER_ERROR, "MONITORING_SOURCE_ERROR", locale);
  }

  /** Returns the stable access-denied contract for missing caregiver consent. */
  @ExceptionHandler(CareRelationshipRequiredException.class)
  public ResponseEntity<ErrorResource> handleDenied(RuntimeException exception, Locale locale) {
    return error(HttpStatus.FORBIDDEN, "CARE_RELATIONSHIP_REQUIRED", locale);
  }

  /** Returns a localized absence response without echoing requested identifiers. */
  @ExceptionHandler({
    FamilyMonitorNotFoundException.class,
    AlertNotFoundException.class,
    ContactChannelNotAvailableException.class
  })
  public ResponseEntity<ErrorResource> handleNotFound(RuntimeException exception, Locale locale) {
    return error(HttpStatus.NOT_FOUND, "NOT_FOUND", locale);
  }

  /** Reports invalid client values without returning raw exception messages. */
  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ErrorResource> handleInvalidRequest(
      IllegalArgumentException exception, Locale locale) {
    return error(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", locale);
  }

  /** Reports resource validation failure without echoing submitted private content. */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResource> handleValidation(
      MethodArgumentNotValidException exception, Locale locale) {
    return error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", locale);
  }

  /** Reports malformed JSON without exposing parser input or internal type details. */
  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ErrorResource> handleMalformedBody(
      HttpMessageNotReadableException exception, Locale locale) {
    return error(HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST", locale);
  }

  /** Reports an unavailable follow-up transition using the stable state-conflict contract. */
  @ExceptionHandler(InvalidAlertTransitionException.class)
  public ResponseEntity<ErrorResource> handleInvalidState(
      InvalidAlertTransitionException exception, Locale locale) {
    return error(HttpStatus.CONFLICT, "INVALID_STATE", locale);
  }

  /** Treats broken internal follow-up invariants as server failures, not caregiver conflicts. */
  @ExceptionHandler(IllegalStateException.class)
  public ResponseEntity<ErrorResource> handleInconsistentState(
      IllegalStateException exception, Locale locale) {
    return error(HttpStatus.INTERNAL_SERVER_ERROR, "MONITORING_STATE_ERROR", locale);
  }

  private ResponseEntity<ErrorResource> error(HttpStatus status, String code, Locale locale) {
    var key = "family.error." + code.toLowerCase(Locale.ROOT).replace('_', '-');
    return ResponseEntity.status(status)
        .body(new ErrorResource(code, messages.getMessage(key, null, locale)));
  }
}
