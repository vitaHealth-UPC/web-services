package com.tata.shared.interfaces.rest.transform;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

/** Builds the common HTTP error envelope; each context owns its error codes and status. */
public final class ProblemDetailAssembler {
    private ProblemDetailAssembler() {}

    public static ProblemDetail from(HttpStatus status, String code, String message) {
        var detail = ProblemDetail.forStatusAndDetail(status, message);
        detail.setTitle(code);
        return detail;
    }
}
