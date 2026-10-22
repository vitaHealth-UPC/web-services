package com.vitahealth.tata.carelink.interfaces.rest.transform;

import com.vitahealth.tata.carelink.domain.model.commands.RegisterConsentCommand;
import com.vitahealth.tata.carelink.interfaces.rest.resources.RegisterConsentResource;
import java.util.Objects;

/** Combines the relationship path and explicit adult decision. */
public final class RegisterConsentCommandFromResourceAssembler {
  private RegisterConsentCommandFromResourceAssembler() {}

  /** Combines the relationship path and explicit adult decision. */
  public static RegisterConsentCommand toCommand(
      String careLinkId, RegisterConsentResource resource) {
    Objects.requireNonNull(resource, "resource");
    if (resource.accepted() == null) {
      throw new IllegalArgumentException("an explicit consent decision is required");
    }
    return new RegisterConsentCommand(careLinkId, resource.accepted());
  }
}
