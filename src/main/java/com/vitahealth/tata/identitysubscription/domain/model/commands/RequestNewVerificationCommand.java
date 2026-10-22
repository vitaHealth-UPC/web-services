package com.vitahealth.tata.identitysubscription.domain.model.commands;

/** Requests renewal of an unverified account's email challenge. */
public record RequestNewVerificationCommand(String email) {
  /** Keeps the recipient out of diagnostic strings. */
  @Override
  public String toString() {
    return "RequestNewVerificationCommand[redacted]";
  }
}
