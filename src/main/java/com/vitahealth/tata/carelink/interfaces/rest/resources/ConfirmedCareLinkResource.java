package com.vitahealth.tata.carelink.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/** Credential-free list response containing one confirmed adult relationship. */
public record ConfirmedCareLinkResource(
    String id,
    String olderAdultId,
    String olderAdultName,
    Instant confirmedAt,
    @Schema(description = "True while the adult is signed in on some device")
        boolean adultSignedIn) {
  /** Keeps adult display information out of diagnostic logging. */
  @Override
  public String toString() {
    return "ConfirmedCareLinkResource[id=" + id + ", personalData=REDACTED]";
  }
}
