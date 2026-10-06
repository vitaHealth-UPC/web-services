package com.tata.treatmentmanagement.interfaces.rest;

import com.tata.treatmentmanagement.application.TreatmentApplicationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = {MedicationsController.class, TreatmentsController.class})
public class TreatmentExceptionHandler {
    @ExceptionHandler(TreatmentApplicationException.class)
    public ProblemDetail handle(TreatmentApplicationException exception) {
        var status = switch (exception.code()) {
            case MEDICATION_NOT_FOUND, TREATMENT_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case MEDICATION_INACTIVE, INCOMPLETE_TREATMENT, INVALID_TRANSITION -> HttpStatus.CONFLICT;
            case CARE_LINK_NOT_AUTHORIZED -> HttpStatus.FORBIDDEN;
        };
        var detail = ProblemDetail.forStatusAndDetail(status, exception.getMessage());
        detail.setTitle(exception.code().name());
        return detail;
    }

    @ExceptionHandler({IllegalArgumentException.class, MethodArgumentNotValidException.class})
    public ProblemDetail validation(Exception exception) {
        var detail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "request validation failed");
        detail.setTitle("VALIDATION_ERROR");
        return detail;
    }
}
