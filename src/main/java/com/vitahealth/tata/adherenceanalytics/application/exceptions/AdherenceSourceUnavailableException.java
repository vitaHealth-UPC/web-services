package com.vitahealth.tata.adherenceanalytics.application.exceptions;

/** Reports failure of a source context independently of analytics request validation. */
public final class AdherenceSourceUnavailableException extends RuntimeException {
  /** Retains the original provider failure for diagnostics and recovery decisions. */
  public AdherenceSourceUnavailableException(RuntimeException cause) {
    super("Adherence source is unavailable", cause);
  }
}
