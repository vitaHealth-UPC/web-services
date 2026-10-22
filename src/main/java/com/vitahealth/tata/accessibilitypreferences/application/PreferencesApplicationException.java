package com.vitahealth.tata.accessibilitypreferences.application;

import com.vitahealth.tata.shared.application.result.ApplicationError;

/** Marks a rejected preference decision independently of infrastructure and transport failures. */
public final class PreferencesApplicationException extends RuntimeException {
  /** Captures a domain validation rejection without exposing submitted preferences or contacts. */
  public PreferencesApplicationException() {
    super("request validation failed");
  }

  /** Converts the expected rejection after the mutation transaction has finished. */
  public ApplicationError toApplicationError() {
    return new ApplicationError("VALIDATION_ERROR", getMessage());
  }
}
