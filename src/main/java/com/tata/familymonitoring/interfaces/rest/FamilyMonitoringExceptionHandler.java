package com.tata.familymonitoring.interfaces.rest;

import com.tata.familymonitoring.domain.exceptions.AlertNotFoundException;
import com.tata.familymonitoring.domain.exceptions.ContactChannelNotAvailableException;
import com.tata.familymonitoring.domain.exceptions.FamilyMonitorNotFoundException;
import com.tata.familymonitoring.interfaces.rest.resources.ErrorResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Turns Family Monitoring failures into stable error codes. */
@RestControllerAdvice(basePackages = "com.tata.familymonitoring")
public class FamilyMonitoringExceptionHandler {
  @ExceptionHandler(com.tata.familymonitoring.application.internal.RequireCareRelationship.AccessDenied.class)
  public ResponseEntity<ErrorResource> handleDenied(RuntimeException exception) {
    return error(HttpStatus.FORBIDDEN, "CARE_RELATIONSHIP_REQUIRED", exception.getMessage());
  }

  @ExceptionHandler({
      FamilyMonitorNotFoundException.class,
      AlertNotFoundException.class,
      ContactChannelNotAvailableException.class})
  public ResponseEntity<ErrorResource> handleNotFound(RuntimeException exception) {
    return error(HttpStatus.NOT_FOUND, "NOT_FOUND", exception.getMessage());
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ErrorResource> handleInvalidRequest(IllegalArgumentException exception) {
    return error(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", exception.getMessage());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResource> handleValidation(MethodArgumentNotValidException exception) {
    String message = exception.getBindingResult().getFieldErrors().stream()
        .map(fieldError -> fieldError.getField() + " " + fieldError.getDefaultMessage())
        .findFirst()
        .orElse("Invalid request");
    return error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message);
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ErrorResource> handleMalformedBody(HttpMessageNotReadableException exception) {
    return error(HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST", "The request body is not valid JSON");
  }

  @ExceptionHandler(IllegalStateException.class)
  public ResponseEntity<ErrorResource> handleInvalidState(IllegalStateException exception) {
    return error(HttpStatus.CONFLICT, "INVALID_STATE", exception.getMessage());
  }

  private ResponseEntity<ErrorResource> error(HttpStatus status, String code, String message) {
    return ResponseEntity.status(status).body(new ErrorResource(code, message));
  }
}
