package com.vitahealth.tata.carelink.interfaces.rest.resources;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/** Profile change fields; actor and target ownership come from authenticated request context. */
public record UpdateOlderAdultProfileResource(
    @NotBlank @Size(max = 160) String fullName, @NotNull LocalDate birthDate) {
  /** Omits submitted personal data from diagnostic output. */
  @Override
  public String toString() {
    return "UpdateOlderAdultProfileResource[personalData=REDACTED]";
  }
}
