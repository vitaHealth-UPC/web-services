package com.vitahealth.tata.shared.interfaces.rest.transform;

import com.vitahealth.tata.shared.application.result.ApplicationError;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

/** Builds the common HTTP error envelope; each context owns its error codes and status. */
public final class ProblemDetailAssembler {
  private ProblemDetailAssembler() {}

  /** Creates a public failure envelope from a context-owned code and message. */
  public static ProblemDetail from(HttpStatus status, String code, String message) {
    return from(status, new ApplicationError(code, message));
  }

  /** Maps an application failure while preserving the existing title/detail HTTP contract. */
  public static ProblemDetail from(HttpStatus status, ApplicationError error) {
    Objects.requireNonNull(status, "status");
    Objects.requireNonNull(error, "error");
    if (!status.isError()) {
      throw new IllegalArgumentException("problem detail requires an HTTP error status");
    }
    var detail = ProblemDetail.forStatusAndDetail(status, error.message());
    detail.setTitle(error.code());
    return detail;
  }
}
