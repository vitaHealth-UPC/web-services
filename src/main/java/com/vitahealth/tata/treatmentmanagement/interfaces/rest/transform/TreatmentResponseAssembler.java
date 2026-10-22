package com.vitahealth.tata.treatmentmanagement.interfaces.rest.transform;

import com.vitahealth.tata.shared.application.result.ApplicationError;
import com.vitahealth.tata.shared.application.result.Result;
import com.vitahealth.tata.shared.interfaces.rest.transform.ProblemDetailAssembler;
import com.vitahealth.tata.treatmentmanagement.application.TreatmentApplicationException;
import java.util.function.Function;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/** Preserves successful resources and maps expected treatment failures to their HTTP envelope. */
public final class TreatmentResponseAssembler {
  private TreatmentResponseAssembler() {}

  /** Applies the endpoint success status or the stable context failure policy. */
  public static <T, R> ResponseEntity<?> toResponse(
      Result<T, ApplicationError> result, HttpStatus successStatus, Function<T, R> resource) {
    return switch (result) {
      case Result.Success<T, ApplicationError> success ->
          ResponseEntity.status(successStatus).body(resource.apply(success.value()));
      case Result.Failure<T, ApplicationError> failure -> {
        var status =
            failureStatus(TreatmentApplicationException.Code.valueOf(failure.error().code()));
        yield ResponseEntity.status(status)
            .body(
                ProblemDetailAssembler.from(
                    status, failure.error().code(), failure.error().message()));
      }
    };
  }

  /** Shares the HTTP status policy between typed outcomes and exception advice. */
  public static HttpStatus failureStatus(TreatmentApplicationException.Code code) {
    return switch (code) {
      case MEDICATION_NOT_FOUND, TREATMENT_NOT_FOUND -> HttpStatus.NOT_FOUND;
      case MEDICATION_INACTIVE, INCOMPLETE_TREATMENT, INVALID_TRANSITION -> HttpStatus.CONFLICT;
      case CARE_LINK_NOT_AUTHORIZED -> HttpStatus.FORBIDDEN;
    };
  }
}
