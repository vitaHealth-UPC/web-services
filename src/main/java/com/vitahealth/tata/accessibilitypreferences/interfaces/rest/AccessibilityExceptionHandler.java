package com.vitahealth.tata.accessibilitypreferences.interfaces.rest;

import com.vitahealth.tata.accessibilitypreferences.interfaces.rest.transform.PreferencesResponseAssembler;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Translates request validation failures through the same localized policy as application
 * rejections.
 */
@RestControllerAdvice(
    assignableTypes = {
      AccessibilityController.class,
      DarkThemeController.class,
      NotificationPreferencesController.class
    })
public class AccessibilityExceptionHandler {
  private final PreferencesResponseAssembler responses;

  /** Injects the context-owned HTTP mapper without coupling application validation to a locale. */
  public AccessibilityExceptionHandler(PreferencesResponseAssembler responses) {
    this.responses = responses;
  }

  /** Retains a stable validation code and omits binding internals from the public response. */
  @ExceptionHandler({
    IllegalArgumentException.class,
    MethodArgumentNotValidException.class,
    HttpMessageNotReadableException.class
  })
  public ProblemDetail validation(Exception exception) {
    return responses.validationProblem();
  }
}
