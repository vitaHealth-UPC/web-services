package com.vitahealth.tata.intakeexecution.application;

import com.vitahealth.tata.shared.application.result.ApplicationError;
import java.util.Objects;

/** Public application failure contract; adapters translate its codes to transport responses. */
public final class IntakeApplicationException extends RuntimeException {
  /** Stable public rejection codes owned by Intake Execution. */
  public enum Code {
    VALIDATION_ERROR,
    INTAKE_NOT_FOUND,
    INTAKE_NOT_CONFIRMABLE,
    VOICE_CONFIRMATION_DISABLED
  }

  private final Code code;

  /** Captures a stable expected rejection without exposing infrastructure diagnostics. */
  public IntakeApplicationException(Code code, String message) {
    super(message);
    this.code = Objects.requireNonNull(code, "code");
  }

  /** Returns the context-owned machine-readable code. */
  public Code code() {
    return code;
  }

  /** Adapts the rejection to the shared application outcome without an HTTP dependency. */
  public ApplicationError toApplicationError() {
    return new ApplicationError(code.name(), getMessage());
  }
}
