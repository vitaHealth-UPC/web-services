package com.vitahealth.tata.shared.interfaces.rest.transform;

import com.vitahealth.tata.shared.application.result.ApplicationError;
import com.vitahealth.tata.shared.application.result.Result;
import java.util.Objects;
import java.util.function.Function;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;

/**
 * Translates application results into HTTP resources at the REST boundary.
 *
 * <p>Adapted from learning-center-platform under the MIT license, preserving Tata's explicit
 * context-specific error mapping and existing problem response contract.
 */
public final class ResponseEntityAssembler {
  private ResponseEntityAssembler() {}

  /**
   * Maps a success through its resource assembler or delegates a failure to the error assembler.
   *
   * @param result application outcome
   * @param successResourceAssembler translation of the successful value
   * @param successStatus successful HTTP status
   * @param failureStatusMapper context-owned mapping of a stable error to HTTP status
   * @param <T> application success value
   * @param <R> REST resource
   * @return success resource or problem response
   */
  public static <T, R> ResponseEntity<?> toResponseEntityFromResult(
      Result<T, ApplicationError> result,
      Function<T, R> successResourceAssembler,
      HttpStatusCode successStatus,
      Function<ApplicationError, HttpStatus> failureStatusMapper) {
    Objects.requireNonNull(result, "result");
    Objects.requireNonNull(successResourceAssembler, "successResourceAssembler");
    Objects.requireNonNull(successStatus, "successStatus");
    Objects.requireNonNull(failureStatusMapper, "failureStatusMapper");
    if (!successStatus.is2xxSuccessful()) {
      throw new IllegalArgumentException("success status must be a successful HTTP status");
    }
    return switch (result) {
      case Result.Success<T, ApplicationError> success ->
          new ResponseEntity<>(successResourceAssembler.apply(success.value()), successStatus);
      case Result.Failure<T, ApplicationError> failure ->
          ErrorResponseAssembler.toErrorResponseFromApplicationError(
              failure.error(), failureStatusMapper.apply(failure.error()));
    };
  }
}
