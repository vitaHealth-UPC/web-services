package com.vitahealth.tata.familymonitoring.application.exceptions;

/** Reports unusable source-context evidence without blaming a valid caregiver request. */
public final class InvalidMonitoringSourceException extends RuntimeException {
  /**
   * Retains the technical cause for internal diagnosis without copying it into the public message.
   */
  public InvalidMonitoringSourceException(RuntimeException cause) {
    super("Monitoring source evidence is inconsistent", cause);
  }
}
