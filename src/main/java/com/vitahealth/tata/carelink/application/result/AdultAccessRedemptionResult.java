package com.vitahealth.tata.carelink.application.result;

import java.time.Instant;

/**
 * Identity restored on the adult's device. When the adult already has a PIN no credential is
 * issued: the device must sign in with it. Without a PIN a session lets the adult create one.
 */
public record AdultAccessRedemptionResult(
    String careLinkId,
    String olderAdultId,
    String olderAdultName,
    boolean pinRegistered,
    String accessToken,
    Instant expiresAt) {
  /** Omits the credential and personal data from diagnostic output. */
  @Override
  public String toString() {
    return "AdultAccessRedemptionResult[careLinkId=" + careLinkId + ", personalData=REDACTED]";
  }
}
