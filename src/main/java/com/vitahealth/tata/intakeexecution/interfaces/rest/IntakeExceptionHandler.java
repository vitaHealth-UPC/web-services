package com.vitahealth.tata.intakeexecution.interfaces.rest;

import com.vitahealth.tata.intakeexecution.application.IntakeApplicationException;
import com.vitahealth.tata.intakeexecution.interfaces.rest.transform.IntakeResponseAssembler;
import com.vitahealth.tata.shared.interfaces.rest.transform.ProblemDetailAssembler;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Preserves public intake rejection envelopes and request validation responses. */
@RestControllerAdvice(assignableTypes = IntakesController.class)
public class IntakeExceptionHandler {
  /** Maps expected context rejections using the typed response policy. */
  @ExceptionHandler(IntakeApplicationException.class)
  public ProblemDetail handleIntake(IntakeApplicationException exception) {
    return IntakeResponseAssembler.toProblem(exception.toApplicationError());
  }

  /** Rejects malformed requests without returning binding internals. */
  @ExceptionHandler({
    IllegalArgumentException.class,
    MethodArgumentNotValidException.class,
    HttpMessageNotReadableException.class
  })
  public ProblemDetail handleValidation(Exception exception) {
    return ProblemDetailAssembler.from(
        HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "request validation failed");
  }
}
