package com.vitahealth.tata.identitysubscription.interfaces.rest.resources;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Caregiver sign-in input with unchanged password content. */
public record SignInResource(
    @NotBlank @Email @Size(max = 200) String email, @NotBlank String password) {
  /** Keeps credentials and the email identifier out of diagnostic strings. */
  @Override
  public String toString() {
    return "SignInResource[redacted]";
  }
}
