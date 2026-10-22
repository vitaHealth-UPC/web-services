package com.vitahealth.tata.identitysubscription.application.exceptions;

/** Signals that a supplied credential cannot be safely represented by the configured hasher. */
public final class UnsupportedCredentialException extends IllegalArgumentException {
  /** Creates a validation failure without retaining the raw credential. */
  public UnsupportedCredentialException() {
    super("Credential cannot be encoded by the configured hashing policy");
  }
}
