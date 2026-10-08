package com.tata.familymonitoring.domain.exceptions;

public class AlertNotFoundException extends RuntimeException {

  public AlertNotFoundException(Long alertId) {
    super("Alert " + alertId + " was not found");
  }
}
