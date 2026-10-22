package com.vitahealth.tata.identitysubscription.domain.model.commands;

/** Account registration input; the plaintext password is used only for credential creation. */
public record RegisterFamilyAccountCommand(String name, String email, String password) {
  /** Avoids accidental credential disclosure through record diagnostics. */
  @Override
  public String toString() {
    return "RegisterFamilyAccountCommand[credentials=<redacted>]";
  }
}
