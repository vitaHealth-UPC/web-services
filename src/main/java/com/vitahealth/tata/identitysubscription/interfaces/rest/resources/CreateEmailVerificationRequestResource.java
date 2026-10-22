package com.vitahealth.tata.identitysubscription.interfaces.rest.resources;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Request to renew an unverified account's challenge through its email identity. */
public record CreateEmailVerificationRequestResource(
    @NotBlank @Email @Size(max = 200) String email) {
  /** Keeps the recipient out of diagnostic strings. */
  @Override
  public String toString() {
    return "CreateEmailVerificationRequestResource[redacted]";
  }
}
