package com.vitahealth.tata.intakeexecution.interfaces.rest.transform;

import com.vitahealth.tata.intakeexecution.application.IntakeApplicationException;
import com.vitahealth.tata.shared.application.result.ApplicationError;
import com.vitahealth.tata.shared.application.result.Result;
import com.vitahealth.tata.shared.interfaces.rest.transform.ProblemDetailAssembler;
import java.util.function.Function;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;

/** Maps expected intake outcomes to HTTP while retaining endpoint-specific empty responses. */
public final class IntakeResponseAssembler {
  private IntakeResponseAssembler() {}

  /** Applies the endpoint success mapping or the shared intake rejection policy. */
  public static <T> ResponseEntity<?> toResponse(
      Result<T, ApplicationError> result, Function<T, ResponseEntity<?>> success) {
    return switch (result) {
      case Result.Success<T, ApplicationError> value -> success.apply(value.value());
      case Result.Failure<T, ApplicationError> failure -> {
        var problem = toProblem(failure.error());
        yield ResponseEntity.status(problem.getStatus()).body(problem);
      }
    };
  }

  /** Reuses the same stable failure status and envelope for typed outcomes and exception advice. */
  public static ProblemDetail toProblem(ApplicationError error) {
    var code = IntakeApplicationException.Code.valueOf(error.code());
    var status =
        switch (code) {
          case VALIDATION_ERROR -> HttpStatus.BAD_REQUEST;
          case INTAKE_NOT_FOUND -> HttpStatus.NOT_FOUND;
          case INTAKE_NOT_CONFIRMABLE, VOICE_CONFIRMATION_DISABLED -> HttpStatus.CONFLICT;
        };
    var message =
        code == IntakeApplicationException.Code.VALIDATION_ERROR
            ? "request validation failed"
            : error.message();
    return ProblemDetailAssembler.from(status, error.code(), message);
  }
}
