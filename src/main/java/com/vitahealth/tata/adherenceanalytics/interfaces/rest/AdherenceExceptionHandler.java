package com.vitahealth.tata.adherenceanalytics.interfaces.rest;

import com.vitahealth.tata.adherenceanalytics.application.exceptions.AdherenceSourceUnavailableException;
import com.vitahealth.tata.adherenceanalytics.application.exceptions.InvalidAdherenceSourceException;
import com.vitahealth.tata.shared.interfaces.rest.transform.ProblemDetailAssembler;
import java.util.Locale;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/** Maps request validation separately from inconsistent source evidence. */
@RestControllerAdvice(assignableTypes = {AdherenceController.class, AdherenceViewController.class})
public class AdherenceExceptionHandler {
  private final MessageSource messages;

  /** Injects localized public error messages shared by both analytics controllers. */
  public AdherenceExceptionHandler(MessageSource messages) {
    this.messages = messages;
  }

  /** Keeps source diagnostics private and reports a server-side evidence failure. */
  @ExceptionHandler({
    InvalidAdherenceSourceException.class,
    AdherenceSourceUnavailableException.class
  })
  public ProblemDetail invalidSource(RuntimeException exception, Locale locale) {
    return ProblemDetailAssembler.from(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "ADHERENCE_SOURCE_ERROR",
        messages.getMessage("adherence.error.source-error", null, locale));
  }

  /** Reports invalid period and owner arguments as client validation failures. */
  @ExceptionHandler({
    IllegalArgumentException.class,
    MethodArgumentTypeMismatchException.class,
    MissingServletRequestParameterException.class
  })
  public ProblemDetail invalidRequest(Exception exception, Locale locale) {
    return ProblemDetailAssembler.from(
        HttpStatus.BAD_REQUEST,
        "VALIDATION_ERROR",
        messages.getMessage("adherence.error.invalid-request", null, locale));
  }
}
