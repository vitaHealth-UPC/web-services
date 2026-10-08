package com.tata.treatmentmanagement.interfaces.rest;

import com.tata.treatmentmanagement.application.TreatmentApplicationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import com.tata.shared.interfaces.rest.transform.ProblemDetailAssembler;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = {MedicationsController.class, TreatmentsController.class, MyMedicationsController.class})
public class TreatmentExceptionHandler {
    @ExceptionHandler(TreatmentApplicationException.class)
    public ProblemDetail handle(TreatmentApplicationException exception) {
        var status = switch (exception.code()) {
            case MEDICATION_NOT_FOUND, TREATMENT_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case MEDICATION_INACTIVE, INCOMPLETE_TREATMENT, INVALID_TRANSITION -> HttpStatus.CONFLICT;
            case CARE_LINK_NOT_AUTHORIZED -> HttpStatus.FORBIDDEN;
        };
        return ProblemDetailAssembler.from(status, exception.code().name(), exception.getMessage());
    }

    @ExceptionHandler({IllegalArgumentException.class, MethodArgumentNotValidException.class})
    public ProblemDetail validation(Exception exception) {
        return ProblemDetailAssembler.from(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "request validation failed");
    }
}
