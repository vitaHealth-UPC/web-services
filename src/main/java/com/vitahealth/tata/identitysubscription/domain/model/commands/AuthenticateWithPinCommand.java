package com.vitahealth.tata.identitysubscription.domain.model.commands;

/** Adult authentication input consumed under the credential's mutation lock. */
public record AuthenticateWithPinCommand(String olderAdultId, String pin) {
  /** Hides the raw PIN and owner identity in diagnostics. */
  @Override
  public String toString() {
    return "AuthenticateWithPinCommand[credentials=<redacted>]";
  }
}
