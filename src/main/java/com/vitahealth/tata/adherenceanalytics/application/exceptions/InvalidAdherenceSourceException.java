package com.vitahealth.tata.adherenceanalytics.application.exceptions;

/** Distinguishes inconsistent source evidence from an invalid analytics request. */
public final class InvalidAdherenceSourceException extends RuntimeException {
  /** Retains the technical cause for diagnostics without exposing it in HTTP responses. */
  public InvalidAdherenceSourceException(RuntimeException cause) {
    super("Adherence source evidence is inconsistent", cause);
  }
}
