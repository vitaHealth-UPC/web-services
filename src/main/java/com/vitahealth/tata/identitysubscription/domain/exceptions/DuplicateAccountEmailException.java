package com.vitahealth.tata.identitysubscription.domain.exceptions;

/** Signals the account email uniqueness conflict without exposing database or personal details. */
public final class DuplicateAccountEmailException extends RuntimeException {
  /** Creates the expected domain persistence conflict with no SQL payload. */
  public DuplicateAccountEmailException() {
    super("An account already uses this email");
  }
}
