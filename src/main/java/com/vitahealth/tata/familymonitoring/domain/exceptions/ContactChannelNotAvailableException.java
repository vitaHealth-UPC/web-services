package com.vitahealth.tata.familymonitoring.domain.exceptions;

/** Reports absence of a configured contact channel for the requested adult. */
public class ContactChannelNotAvailableException extends RuntimeException {

  public ContactChannelNotAvailableException(String olderAdultId) {
    super("No contact channel is available for older adult " + olderAdultId);
  }
}
