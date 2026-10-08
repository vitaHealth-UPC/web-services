package com.tata.familymonitoring.domain.exceptions;

public class ContactChannelNotAvailableException extends RuntimeException {

  public ContactChannelNotAvailableException(String olderAdultId) {
    super("No contact channel is available for older adult " + olderAdultId);
  }
}
