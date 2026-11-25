package com.vitahealth.tata.carelink.interfaces.rest.resources;

import com.vitahealth.tata.carelink.domain.model.valueobjects.LinkingCode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Code typed on the adult's device. */
public record RedeemAdultAccessCodeResource(
    @NotBlank @Size(max = LinkingCode.MAX_LENGTH) String code) {
  /** Keeps the code out of diagnostic output. */
  @Override
  public String toString() {
    return "RedeemAdultAccessCodeResource[code=REDACTED]";
  }
}
