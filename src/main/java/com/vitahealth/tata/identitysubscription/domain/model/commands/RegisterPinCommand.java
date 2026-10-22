package com.vitahealth.tata.identitysubscription.domain.model.commands;

/** Enrollment input for the adult's PIN; the raw credential is never persisted. */
public record RegisterPinCommand(String olderAdultId, String pin) {
  /** Hides the raw PIN and owner identity in diagnostics. */
  @Override
  public String toString() {
    return "RegisterPinCommand[credentials=<redacted>]";
  }
}
