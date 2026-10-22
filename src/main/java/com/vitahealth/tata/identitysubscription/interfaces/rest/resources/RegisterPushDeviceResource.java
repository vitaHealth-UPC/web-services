package com.vitahealth.tata.identitysubscription.interfaces.rest.resources;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Token-only device input; account and session identities cannot be supplied by the client. */
public record RegisterPushDeviceResource(@NotBlank @Size(max = 4096) String token) {
  /** Prevents automatic request diagnostics from exposing an opaque device address. */
  @Override
  public String toString() {
    return "RegisterPushDeviceResource[redacted]";
  }
}
