package com.vitahealth.tata.identitysubscription.domain.exceptions;

/** Signals that an adult already owns a PIN credential without exposing persistence details. */
public final class DuplicatePinCredentialException extends RuntimeException {
  /** Creates the expected enrollment conflict with no PIN or SQL payload. */
  public DuplicatePinCredentialException() {
    super("PIN already registered");
  }
}
