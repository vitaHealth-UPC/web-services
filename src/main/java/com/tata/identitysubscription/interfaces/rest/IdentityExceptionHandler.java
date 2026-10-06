package com.tata.identitysubscription.interfaces.rest;

import com.tata.identitysubscription.application.internal.IdentityApplicationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = {AccountsController.class, SessionsController.class})
public class IdentityExceptionHandler {
    @ExceptionHandler(IdentityApplicationException.class)
    public ProblemDetail handleIdentity(IdentityApplicationException exception) {
        var status = switch (exception.code()) {
            case DUPLICATE_EMAIL -> HttpStatus.CONFLICT;
            case ACCOUNT_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case INVALID_VERIFICATION, INVALID_CREDENTIALS -> HttpStatus.UNAUTHORIZED;
            case VERIFICATION_EXPIRED -> HttpStatus.GONE;
            case ACCOUNT_NOT_ACTIVE -> HttpStatus.FORBIDDEN;
        };
        var detail = ProblemDetail.forStatusAndDetail(status, exception.getMessage());
        detail.setTitle(exception.code().name());
        return detail;
    }

    @ExceptionHandler({IllegalArgumentException.class, MethodArgumentNotValidException.class})
    public ProblemDetail handleValidation(Exception exception) {
        var detail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "request validation failed");
        detail.setTitle("VALIDATION_ERROR");
        return detail;
    }
}
