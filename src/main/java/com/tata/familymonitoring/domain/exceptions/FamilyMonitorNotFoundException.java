package com.tata.familymonitoring.domain.exceptions;

public class FamilyMonitorNotFoundException extends RuntimeException {

  public FamilyMonitorNotFoundException(String olderAdultId) {
    super("No family monitor exists for older adult " + olderAdultId);
  }
}
