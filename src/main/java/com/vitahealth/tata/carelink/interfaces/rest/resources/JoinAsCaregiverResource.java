package com.vitahealth.tata.carelink.interfaces.rest.resources;

import com.vitahealth.tata.carelink.domain.model.valueobjects.LinkingCode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Caregiver who uses another caregiver's invitation code. */
public record JoinAsCaregiverResource(
    @NotBlank String caregiverId, @NotBlank @Size(max = LinkingCode.MAX_LENGTH) String code) {
  /** Keeps the code out of diagnostic output. */
  @Override
  public String toString() {
    return "JoinAsCaregiverResource[code=REDACTED]";
  }
}
