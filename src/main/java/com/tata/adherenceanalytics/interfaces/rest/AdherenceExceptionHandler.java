package com.tata.adherenceanalytics.interfaces.rest;

import com.tata.shared.interfaces.rest.transform.ProblemDetailAssembler;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = AdherenceController.class)
public class AdherenceExceptionHandler {
    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail invalidRequest(IllegalArgumentException exception) {
        return ProblemDetailAssembler.from(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", exception.getMessage());
    }
}
