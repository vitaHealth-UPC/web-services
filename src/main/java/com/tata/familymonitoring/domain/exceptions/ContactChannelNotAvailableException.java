package com.tata.familymonitoring.domain.exceptions;

public class ContactChannelNotAvailableException extends RuntimeException {

  public ContactChannelNotAvailableException(Long olderAdultId) {
    super("No contact channel is available for older adult " + olderAdultId);
  }
}
