package com.vitahealth.tata.identitysubscription.interfaces.rest.resources;

import jakarta.validation.constraints.*;

public record CompletePasswordRecoveryResource(
    @NotBlank @Email @Size(max = 200) String email,
    @NotBlank @Pattern(regexp = "[0-9]{6}") String code,
    @NotBlank @Size(min = 8, max = 72) String newPassword) {
  @Override
  public String toString() {
    return "CompletePasswordRecoveryResource[personalData=REDACTED]";
  }
}
