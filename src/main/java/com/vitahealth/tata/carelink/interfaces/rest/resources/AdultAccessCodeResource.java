package com.vitahealth.tata.carelink.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/** Sign-in code to share with the adult. */
public record AdultAccessCodeResource(
    String careLinkId, @Schema(example = "K7PM-Q2XD") String code, Instant expiresAt) {
  /** Keeps the code out of diagnostic output. */
  @Override
  public String toString() {
    return "AdultAccessCodeResource[careLinkId=" + careLinkId + ", code=REDACTED]";
  }
}
