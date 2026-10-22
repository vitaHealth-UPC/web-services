package com.vitahealth.tata.shared.interfaces.rest.transform;

import com.vitahealth.tata.shared.application.result.ApplicationError;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;

/**
 * Adapts application failures to Tata's existing HTTP error envelope.
 *
 * <p>Adapted from learning-center-platform under the MIT license. The receiving context chooses the
 * status explicitly because its business error codes do not encode HTTP status uniformly.
 */
public final class ErrorResponseAssembler {
  private ErrorResponseAssembler() {}

  /**
   * Builds a failure response without exposing internal diagnostic details.
   *
   * @param error application failure with its stable code and public message
   * @param status status selected by the context's error mapping
   * @return the existing title/detail problem response
   */
  public static ResponseEntity<ProblemDetail> toErrorResponseFromApplicationError(
      ApplicationError error, HttpStatus status) {
    Objects.requireNonNull(error, "error");
    Objects.requireNonNull(status, "status");
    if (!status.isError()) {
      throw new IllegalArgumentException("failure status must be an HTTP error");
    }
    return ResponseEntity.status(status).body(ProblemDetailAssembler.from(status, error));
  }
}
