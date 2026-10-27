package com.vitahealth.tata.identitysubscription.interfaces.rest.resources;

import jakarta.validation.constraints.*;

public record RequestPasswordRecoveryResource(@NotBlank @Email @Size(max = 200) String email) {
  @Override
  public String toString() {
    return "RequestPasswordRecoveryResource[personalData=REDACTED]";
  }
}
