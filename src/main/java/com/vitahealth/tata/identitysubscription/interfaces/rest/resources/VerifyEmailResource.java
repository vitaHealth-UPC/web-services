package com.vitahealth.tata.identitysubscription.interfaces.rest.resources;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Email verification input containing the current six-digit ASCII challenge. */
public record VerifyEmailResource(
    @NotBlank @Email @Size(max = 200) String email,
    @NotBlank @Pattern(regexp = "[0-9]{6}") String code) {
  /** Omits the recipient and secret challenge from diagnostic strings. */
  @Override
  public String toString() {
    return "VerifyEmailResource[redacted]";
  }
}
