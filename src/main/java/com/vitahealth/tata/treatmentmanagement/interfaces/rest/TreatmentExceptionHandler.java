package com.vitahealth.tata.treatmentmanagement.interfaces.rest;

import com.vitahealth.tata.shared.interfaces.rest.transform.ProblemDetailAssembler;
import com.vitahealth.tata.treatmentmanagement.application.TreatmentApplicationException;
import com.vitahealth.tata.treatmentmanagement.interfaces.rest.transform.TreatmentResponseAssembler;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/** Maps expected treatment failures and malformed inputs to the common public problem envelope. */
@RestControllerAdvice(
    assignableTypes = {
      MedicationsController.class,
      TreatmentsController.class,
      MyMedicationsController.class
    })
public class TreatmentExceptionHandler {
  /** Preserves the context-owned failure code and its defined HTTP status. */
  @ExceptionHandler(TreatmentApplicationException.class)
  public ProblemDetail handle(TreatmentApplicationException exception) {
    var status = TreatmentResponseAssembler.failureStatus(exception.code());
    return ProblemDetailAssembler.from(status, exception.code().name(), exception.getMessage());
  }

  /** Returns a safe validation message without exposing submitted clinical or credential data. */
  @ExceptionHandler({
    IllegalArgumentException.class,
    MethodArgumentNotValidException.class,
    HttpMessageNotReadableException.class,
    MissingServletRequestParameterException.class,
    MethodArgumentTypeMismatchException.class
  })
  public ProblemDetail validation(Exception exception) {
    return ProblemDetailAssembler.from(
        HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "request validation failed");
  }
}
