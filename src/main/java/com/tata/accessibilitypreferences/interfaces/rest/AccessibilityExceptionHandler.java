package com.tata.accessibilitypreferences.interfaces.rest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import com.tata.shared.interfaces.rest.transform.ProblemDetailAssembler;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = {AccessibilityController.class, NotificationPreferencesController.class})
public class AccessibilityExceptionHandler {

    @ExceptionHandler({
            IllegalArgumentException.class,
            MethodArgumentNotValidException.class,
            HttpMessageNotReadableException.class})
    public ProblemDetail validation(Exception exception) {
        return ProblemDetailAssembler.from(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "request validation failed");
    }
}
