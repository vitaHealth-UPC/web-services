package com.tata.identitysubscription.interfaces.rest;

import com.tata.identitysubscription.application.IdentityApplicationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import com.tata.shared.interfaces.rest.transform.ProblemDetailAssembler;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = {
        AccountsController.class,
        EmailVerificationRequestsController.class,
        SessionsController.class,
        PinCredentialsController.class,
        PinSessionsController.class,
        SubscriptionsController.class
})
public class IdentityExceptionHandler {
    @ExceptionHandler(IdentityApplicationException.class)
    public ProblemDetail handleIdentity(IdentityApplicationException exception) {
        var status = switch (exception.code()) {
            case DUPLICATE_EMAIL, PIN_ALREADY_REGISTERED -> HttpStatus.CONFLICT;
            case ACCOUNT_NOT_FOUND, PIN_NOT_FOUND, PLAN_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case INVALID_VERIFICATION, INVALID_CREDENTIALS, INVALID_PIN -> HttpStatus.UNAUTHORIZED;
            case VERIFICATION_EXPIRED -> HttpStatus.GONE;
            case ACCOUNT_NOT_ACTIVE, PIN_LOCKED -> HttpStatus.FORBIDDEN;
        };
        return ProblemDetailAssembler.from(status, exception.code().name(), exception.getMessage());
    }

    @ExceptionHandler({IllegalArgumentException.class, MethodArgumentNotValidException.class})
    public ProblemDetail handleValidation(Exception exception) {
        return ProblemDetailAssembler.from(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "request validation failed");
    }
}
