package com.vitahealth.tata.identitysubscription.domain.model.commands;

/** Caregiver login input, independent of HTTP or credential encoding. */
public record AuthenticateFamilyCommand(String email, String password) {
  /** Avoids logging a supplied password or personal email address. */
  @Override
  public String toString() {
    return "AuthenticateFamilyCommand[credentials=<redacted>]";
  }
}
