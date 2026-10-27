package com.vitahealth.tata.identitysubscription.domain.model.commands;

/** Transient recovery input. Neither secret is included in diagnostics. */
public record CompletePasswordRecoveryCommand(String email, String code, String newPassword) {
  @Override
  public String toString() {
    return "CompletePasswordRecoveryCommand[personalData=REDACTED]";
  }
}
