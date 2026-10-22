package com.vitahealth.tata.identitysubscription.interfaces.rest.resources;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Registration input; byte-length limits for BCrypt are enforced by the hashing adapter. */
public record RegisterAccountResource(
    @NotBlank @Size(max = 120) String name,
    @NotBlank @Email @Size(max = 200) String email,
    @NotBlank @Size(min = 8, max = 72) String password) {
  /** Prevents accidental disclosure of registration credentials and personal data. */
  @Override
  public String toString() {
    return "RegisterAccountResource[redacted]";
  }
}
