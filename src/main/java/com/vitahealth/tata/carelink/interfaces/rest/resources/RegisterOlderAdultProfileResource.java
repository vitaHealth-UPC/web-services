package com.vitahealth.tata.carelink.interfaces.rest.resources;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/** Validated registration input whose text limits match domain and persistence contracts. */
public record RegisterOlderAdultProfileResource(
    @NotBlank String caregiverId,
    @NotBlank @Size(max = 160) String fullName,
    @NotNull LocalDate birthDate,
    @Size(max = 160) String emergencyContactName,
    @Size(max = 80) String emergencyContactRelationship,
    @Size(max = 40) String emergencyContactPhone) {
  /** Omits personal data and credentials from diagnostic output. */
  @Override
  public String toString() {
    return "RegisterOlderAdultProfileResource[personalData=REDACTED]";
  }
}
