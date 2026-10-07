package com.tata.intakeexecution.interfaces.rest;

import com.tata.intakeexecution.application.internal.IntakeApplicationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
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
        var detail = ProblemDetail.forStatusAndDetail(status, exception.getMessage());
        detail.setTitle(exception.code().name());
        return detail;
    }

    @ExceptionHandler({
            IllegalArgumentException.class,
            MethodArgumentNotValidException.class,
            HttpMessageNotReadableException.class
    })
    public ProblemDetail handleValidation(Exception exception) {
        var detail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "request validation failed");
        detail.setTitle("VALIDATION_ERROR");
        return detail;
    }
}
