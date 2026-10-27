package com.vitahealth.tata.identitysubscription.domain.model.commands;

public record RequestPasswordRecoveryCommand(String email) {
  @Override
  public String toString() {
    return "RequestPasswordRecoveryCommand[personalData=REDACTED]";
  }
}
