package com.vitahealth.tata.carelink.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/** Identity restored on the adult's device and, without a PIN, a session to create one. */
public record AdultAccessRedemptionResource(
    String careLinkId,
    String olderAdultId,
    String olderAdultName,
    @Schema(description = "When true the device must sign in with the adult's PIN")
        boolean pinRegistered,
    String accessToken,
    Instant expiresAt) {
  /** Omits the credential and personal data from diagnostic output. */
  @Override
  public String toString() {
    return "AdultAccessRedemptionResource[careLinkId=" + careLinkId + ", personalData=REDACTED]";
  }
}
