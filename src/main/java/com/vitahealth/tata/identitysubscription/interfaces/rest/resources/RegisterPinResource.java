package com.vitahealth.tata.identitysubscription.interfaces.rest.resources;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** PIN enrollment input for an authorized adult relationship. */
public record RegisterPinResource(
    @NotBlank String olderAdultId, @NotBlank @Pattern(regexp = "[0-9]{4}") String pin) {
  /** Prevents accidental disclosure of the PIN. */
  @Override
  public String toString() {
    return "RegisterPinResource[redacted]";
  }
}
