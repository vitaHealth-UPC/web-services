package com.vitahealth.tata.carelink.interfaces.rest.transform;

import com.vitahealth.tata.shared.application.result.ApplicationError;
import com.vitahealth.tata.shared.infrastructure.i18n.ApplicationErrorMessageResolver;
import com.vitahealth.tata.shared.interfaces.rest.transform.ProblemDetailAssembler;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;

/**
 * Localizes context public failures while preserving the shared problem envelope and error code.
 */
@Component
public class CareLinkProblemDetailFromErrorAssembler {
  private final ApplicationErrorMessageResolver messages;

  /**
   * Injects shared message resolution without coupling application handlers to request language.
   */
  public CareLinkProblemDetailFromErrorAssembler(ApplicationErrorMessageResolver messages) {
    this.messages = messages;
  }

  /** Converts public application errors using the language resolved for the current request. */
  public ProblemDetail toResource(HttpStatus status, ApplicationError error) {
    return ProblemDetailAssembler.from(
        status, messages.resolve("carelink.error", error, LocaleContextHolder.getLocale()));
  }
}
