package com.tata.intakeexecution.interfaces.rest;

import com.tata.intakeexecution.application.internal.IntakeApplicationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import com.tata.shared.interfaces.rest.transform.ProblemDetailAssembler;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = IntakesController.class)
public class IntakeExceptionHandler {
    @ExceptionHandler(IntakeApplicationException.class)
    public ProblemDetail handleIntake(IntakeApplicationException exception) {
        var status = switch (exception.code()) {
            case INTAKE_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case INTAKE_NOT_CONFIRMABLE, VOICE_CONFIRMATION_DISABLED -> HttpStatus.CONFLICT;
        };
        return ProblemDetailAssembler.from(status, exception.code().name(), exception.getMessage());
    }

    @ExceptionHandler({
            IllegalArgumentException.class,
            MethodArgumentNotValidException.class,
            HttpMessageNotReadableException.class
    })
    public ProblemDetail handleValidation(Exception exception) {
        return ProblemDetailAssembler.from(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "request validation failed");
    }
}
