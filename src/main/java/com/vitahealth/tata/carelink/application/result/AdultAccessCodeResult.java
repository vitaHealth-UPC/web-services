package com.vitahealth.tata.carelink.application.result;

import java.time.Instant;

/** Newly issued code the caregiver shares with the adult. */
public record AdultAccessCodeResult(String careLinkId, String code, Instant expiresAt) {
  /** Keeps the code out of diagnostic output. */
  @Override
  public String toString() {
    return "AdultAccessCodeResult[careLinkId=" + careLinkId + ", code=REDACTED]";
  }
}
