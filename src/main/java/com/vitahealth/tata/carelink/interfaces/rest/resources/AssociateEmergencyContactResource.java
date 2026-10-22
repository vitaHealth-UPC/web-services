package com.vitahealth.tata.carelink.interfaces.rest.resources;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Profile change fields; actor and target ownership come from authenticated request context. */
public record AssociateEmergencyContactResource(
    @NotBlank @Size(max = 160) String fullName,
    @NotBlank @Size(max = 80) String relationship,
    @NotBlank @Size(max = 40) String phone) {
  /** Omits submitted personal data from diagnostic output. */
  @Override
  public String toString() {
    return "AssociateEmergencyContactResource[personalData=REDACTED]";
  }
}
