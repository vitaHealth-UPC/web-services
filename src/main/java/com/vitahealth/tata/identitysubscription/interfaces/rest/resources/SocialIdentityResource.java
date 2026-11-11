package com.vitahealth.tata.identitysubscription.interfaces.rest.resources;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Firebase bearer credential only; email and account identity are never accepted from the client. */
public record SocialIdentityResource(
    @NotBlank @Size(max = 16384) String firebaseIdToken) {
  @Override public String toString() { return "SocialIdentityResource[firebaseIdToken=REDACTED]"; }
}
