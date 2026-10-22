package com.vitahealth.tata.adherenceanalytics.domain.exceptions;

/** Identifies only a concurrent insertion of the same immutable snapshot identity. */
public final class AdherenceSnapshotAlreadyExistsException extends RuntimeException {
  /**
   * Retains provider diagnostics for internal diagnosis after the writer transaction rolls back.
   */
  public AdherenceSnapshotAlreadyExistsException(RuntimeException cause) {
    super("Adherence snapshot already exists", cause);
  }
}
