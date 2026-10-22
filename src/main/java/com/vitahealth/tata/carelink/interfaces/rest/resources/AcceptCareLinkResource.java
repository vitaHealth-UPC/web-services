package com.vitahealth.tata.carelink.interfaces.rest.resources;

import com.vitahealth.tata.carelink.domain.model.valueobjects.LinkingCode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Supplied linking evidence for the public acceptance operation. */
public record AcceptCareLinkResource(
    @NotBlank String caregiverId, @NotBlank @Size(max = LinkingCode.MAX_LENGTH) String code) {
  /** Omits personal data and credentials from diagnostic output. */
  @Override
  public String toString() {
    return "AcceptCareLinkResource[code=REDACTED]";
  }
}
