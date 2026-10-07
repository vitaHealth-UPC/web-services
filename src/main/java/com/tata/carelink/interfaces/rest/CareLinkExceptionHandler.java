package com.tata.carelink.interfaces.rest;

import com.tata.carelink.application.internal.CareLinkApplicationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = {
        OlderAdultsController.class,
        CareLinksController.class
})
public class CareLinkExceptionHandler {
    @ExceptionHandler(CareLinkApplicationException.class)
    public ProblemDetail handleCareLink(CareLinkApplicationException exception) {
        var status = switch (exception.code()) {
            case ACCOUNT_NOT_ENABLED, CONSENT_REQUIRED, CARE_LINK_REVOKED -> HttpStatus.FORBIDDEN;
            case OLDER_ADULT_NOT_FOUND, CARE_LINK_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case INVALID_LINKING_CODE -> HttpStatus.BAD_REQUEST;
            case LINKING_CODE_EXPIRED_OR_USED -> HttpStatus.GONE;
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
