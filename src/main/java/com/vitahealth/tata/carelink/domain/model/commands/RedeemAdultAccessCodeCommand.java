package com.vitahealth.tata.carelink.domain.model.commands;

/** Adult device request to sign in with a caregiver-issued code. */
public record RedeemAdultAccessCodeCommand(String code) {
  /** Keeps the code out of diagnostic output. */
  @Override
  public String toString() {
    return "RedeemAdultAccessCodeCommand[code=REDACTED]";
  }
}
