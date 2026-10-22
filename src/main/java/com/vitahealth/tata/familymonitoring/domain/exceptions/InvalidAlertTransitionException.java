package com.vitahealth.tata.familymonitoring.domain.exceptions;

/** Rejects a caregiver alert transition that is unavailable from its current business state. */
public final class InvalidAlertTransitionException extends IllegalStateException {
  /** Records the violated transition rule without coupling the domain to HTTP or language. */
  public InvalidAlertTransitionException(String reason) {
    super(reason);
  }
}
