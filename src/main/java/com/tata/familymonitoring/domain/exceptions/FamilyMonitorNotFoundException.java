package com.tata.familymonitoring.domain.exceptions;

public class FamilyMonitorNotFoundException extends RuntimeException {

  public FamilyMonitorNotFoundException(Long olderAdultId) {
    super("No family monitor exists for older adult " + olderAdultId);
  }
}
