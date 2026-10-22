package com.vitahealth.tata.carelink.interfaces.rest;

import com.vitahealth.tata.carelink.application.CareLinkApplicationException;
import com.vitahealth.tata.carelink.interfaces.rest.transform.CareLinkProblemDetailFromErrorAssembler;
import com.vitahealth.tata.carelink.interfaces.rest.transform.CareLinkResponseAssembler;
import com.vitahealth.tata.shared.application.result.ApplicationError;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Maps context-owned failures to stable public problem responses without internal diagnostics. */
@RestControllerAdvice(assignableTypes = {OlderAdultsController.class, CareLinksController.class})
public class CareLinkExceptionHandler {
  private final CareLinkProblemDetailFromErrorAssembler problems;

  /** Injects the context HTTP assembler using shared message and problem contracts. */
  public CareLinkExceptionHandler(CareLinkProblemDetailFromErrorAssembler problems) {
    this.problems = problems;
  }

  /**
   * Preserves business codes while mapping retryable allocation failures to service unavailable.
   */
  @ExceptionHandler(CareLinkApplicationException.class)
  public ProblemDetail handleCareLink(CareLinkApplicationException exception) {
    var status = CareLinkResponseAssembler.failureStatus(exception.code());
    return problems.toResource(status, exception.toApplicationError());
  }

  /** Converts transport validation errors without echoing rejected personal or credential input. */
  @ExceptionHandler({
    IllegalArgumentException.class,
    MethodArgumentNotValidException.class,
    HttpMessageNotReadableException.class
  })
  public ProblemDetail handleValidation(Exception exception) {
    return problems.toResource(
        HttpStatus.BAD_REQUEST,
        new ApplicationError("VALIDATION_ERROR", "request validation failed"));
  }
}
