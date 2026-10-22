package com.vitahealth.tata.identitysubscription.interfaces.rest.resources;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** Adult sign-in input; requires the same ASCII PIN format as enrollment. */
public record PinSignInResource(
    @NotBlank String olderAdultId, @NotBlank @Pattern(regexp = "[0-9]{4}") String pin) {
  /** Prevents accidental disclosure of the PIN. */
  @Override
  public String toString() {
    return "PinSignInResource[redacted]";
  }
}
