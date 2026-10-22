package com.vitahealth.tata.identitysubscription.domain.model.commands;

/** Requests verification with transient credentials validated by the command handler. */
public record VerifyEmailCommand(String email, String code) {
  /** Omits recipient and verification secret from diagnostic representations. */
  @Override
  public String toString() {
    return "VerifyEmailCommand[redacted]";
  }
}
