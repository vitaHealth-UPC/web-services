package com.vitahealth.tata.treatmentmanagement.application;

import com.vitahealth.tata.shared.application.result.ApplicationError;
import java.util.Objects;

/** Expected treatment rejection with a stable application-owned error code. */
public final class TreatmentApplicationException extends RuntimeException {
  /** Machine-readable failures independent of transport and request language. */
  public enum Code {
    MEDICATION_NOT_FOUND,
    TREATMENT_NOT_FOUND,
    MEDICATION_INACTIVE,
    INCOMPLETE_TREATMENT,
    INVALID_TRANSITION,
    CARE_LINK_NOT_AUTHORIZED
  }

  private final Code code;

  /** Creates an expected rejection without discarding its stable identifier. */
  public TreatmentApplicationException(Code code, String message) {
    super(message);
    this.code = Objects.requireNonNull(code, "code");
  }

  /** Returns the stable business failure identifier. */
  public Code code() {
    return code;
  }

  /** Converts this rejection to the shared public application result contract. */
  public ApplicationError toApplicationError() {
    return new ApplicationError(code.name(), getMessage());
  }
}
