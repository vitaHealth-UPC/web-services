package com.vitahealth.tata.carelink.application;

import com.vitahealth.tata.shared.application.result.ApplicationError;
import java.util.Objects;

/** Public application failure contract; adapters translate its codes to transport responses. */
public final class CareLinkApplicationException extends RuntimeException {
  /** Stable context-owned failure identifiers independent of HTTP status and request language. */
  public enum Code {
    ACCOUNT_NOT_ENABLED,
    OLDER_ADULT_NOT_FOUND,
    CARE_LINK_NOT_FOUND,
    INVALID_LINKING_CODE,
    LINKING_CODE_EXPIRED_OR_USED,
    CONSENT_REQUIRED,
    CARE_LINK_REVOKED,
    CARE_LINK_ALREADY_EXISTS,
    LINKING_CODE_UNAVAILABLE
  }

  private final Code code;

  /** Creates an expected public business failure with its explicit machine-readable code. */
  public CareLinkApplicationException(Code code, String message) {
    super(message);
    this.code = Objects.requireNonNull(code, "code");
  }

  /** Returns the stable business failure identifier. */
  public Code code() {
    return code;
  }

  /** Converts this expected business rejection to the shared application failure contract. */
  public ApplicationError toApplicationError() {
    return new ApplicationError(code.name(), getMessage());
  }
}
