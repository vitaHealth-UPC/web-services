package com.vitahealth.tata.familymonitoring.domain.exceptions;

/** Reports absence of the requested adult monitoring aggregate. */
public class FamilyMonitorNotFoundException extends RuntimeException {

  public FamilyMonitorNotFoundException(String olderAdultId) {
    super("No family monitor exists for older adult " + olderAdultId);
  }
}
