package com.vitahealth.tata.familymonitoring.domain.exceptions;

/** Reports that the requested alert is absent from the selected monitoring aggregate. */
public class AlertNotFoundException extends RuntimeException {

  public AlertNotFoundException(Long alertId) {
    super("Alert " + alertId + " was not found");
  }
}
