package com.vitahealth.tata.accessibilitypreferences.interfaces.rest.transform;

import com.vitahealth.tata.accessibilitypreferences.application.result.UserPreferencesResult;
import com.vitahealth.tata.shared.application.result.ApplicationError;
import com.vitahealth.tata.shared.application.result.Result;
import com.vitahealth.tata.shared.infrastructure.i18n.ApplicationErrorMessageResolver;
import com.vitahealth.tata.shared.interfaces.rest.transform.ProblemDetailAssembler;
import com.vitahealth.tata.shared.interfaces.rest.transform.ResponseEntityAssembler;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

/** Maps typed preference changes to the established resource or shared problem representation. */
@Component
public final class PreferencesResponseAssembler {
  private final ApplicationErrorMessageResolver messages;

  /**
   * Injects shared message resolution while retaining context-owned HTTP status and error codes.
   */
  public PreferencesResponseAssembler(ApplicationErrorMessageResolver messages) {
    this.messages = messages;
  }

  /** Preserves success200 and validation400; unknown application failures are server failures. */
  public ResponseEntity<?> toResponse(Result<UserPreferencesResult, ApplicationError> result) {
    return ResponseEntityAssembler.toResponseEntityFromResult(
        result.mapError(this::localize),
        UserPreferencesResourceFromResultAssembler::toResource,
        HttpStatus.OK,
        error ->
            "VALIDATION_ERROR".equals(error.code())
                ? HttpStatus.BAD_REQUEST
                : HttpStatus.INTERNAL_SERVER_ERROR);
  }

  /** Uses the same localized validation envelope for binding failures and rejected decisions. */
  public ProblemDetail validationProblem() {
    return ProblemDetailAssembler.from(
        HttpStatus.BAD_REQUEST,
        localize(new ApplicationError("VALIDATION_ERROR", "request validation failed")));
  }

  private ApplicationError localize(ApplicationError error) {
    return messages.resolve("preferences.error", error, LocaleContextHolder.getLocale());
  }
}
