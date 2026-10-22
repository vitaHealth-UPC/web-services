package com.vitahealth.tata.carelink.interfaces.rest.transform;

import com.vitahealth.tata.carelink.application.CareLinkApplicationException;
import com.vitahealth.tata.shared.application.result.ApplicationError;
import com.vitahealth.tata.shared.application.result.Result;
import java.util.function.Function;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

/** Maps application results to resources and localized context-owned HTTP failures. */
@Component
public class CareLinkResponseAssembler {
  private final CareLinkProblemDetailFromErrorAssembler problems;

  /** Injects the localized context failure envelope. */
  public CareLinkResponseAssembler(CareLinkProblemDetailFromErrorAssembler problems) {
    this.problems = problems;
  }

  /**
   * Preserves successful bodies and turns application failures into localized problem responses.
   */
  public <T, R> ResponseEntity<?> toResponse(
      Result<T, ApplicationError> result, Function<T, R> resource) {
    return toResponse(result, HttpStatus.OK, resource);
  }

  /** Uses the endpoint's success status while preserving the public failure contract. */
  public <T, R> ResponseEntity<?> toResponse(
      Result<T, ApplicationError> result, HttpStatus successStatus, Function<T, R> resource) {
    return switch (result) {
      case Result.Success<T, ApplicationError> success ->
          ResponseEntity.status(successStatus).body(resource.apply(success.value()));
      case Result.Failure<T, ApplicationError> failure -> {
        var status =
            failureStatus(CareLinkApplicationException.Code.valueOf(failure.error().code()));
        yield ResponseEntity.status(status).body(problems.toResource(status, failure.error()));
      }
    };
  }

  /** Emits no body for a successful operation while retaining localized failure details. */
  public <T> ResponseEntity<?> toNoContent(Result<T, ApplicationError> result) {
    return switch (result) {
      case Result.Success<T, ApplicationError> success -> ResponseEntity.noContent().build();
      case Result.Failure<T, ApplicationError> failure -> {
        var status =
            failureStatus(CareLinkApplicationException.Code.valueOf(failure.error().code()));
        yield ResponseEntity.status(status).body(problems.toResource(status, failure.error()));
      }
    };
  }

  /** Defines one status policy for typed failures and exceptions at transport boundaries. */
  public static HttpStatus failureStatus(CareLinkApplicationException.Code code) {
    return switch (code) {
      case ACCOUNT_NOT_ENABLED, CONSENT_REQUIRED, CARE_LINK_REVOKED -> HttpStatus.FORBIDDEN;
      case OLDER_ADULT_NOT_FOUND, CARE_LINK_NOT_FOUND -> HttpStatus.NOT_FOUND;
      case CARE_LINK_ALREADY_EXISTS -> HttpStatus.CONFLICT;
      case INVALID_LINKING_CODE -> HttpStatus.BAD_REQUEST;
      case LINKING_CODE_EXPIRED_OR_USED -> HttpStatus.GONE;
      case LINKING_CODE_UNAVAILABLE -> HttpStatus.SERVICE_UNAVAILABLE;
    };
  }
}
