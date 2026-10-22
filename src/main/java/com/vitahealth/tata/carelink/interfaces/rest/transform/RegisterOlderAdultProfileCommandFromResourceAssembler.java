package com.vitahealth.tata.carelink.interfaces.rest.transform;

import com.vitahealth.tata.carelink.domain.model.commands.RegisterOlderAdultProfileCommand;
import com.vitahealth.tata.carelink.interfaces.rest.resources.RegisterOlderAdultProfileResource;

/** Converts validated adult registration input into its domain command. */
public final class RegisterOlderAdultProfileCommandFromResourceAssembler {
  private RegisterOlderAdultProfileCommandFromResourceAssembler() {}

  /** Converts validated adult registration input into its domain command. */
  public static RegisterOlderAdultProfileCommand toCommand(
      RegisterOlderAdultProfileResource resource) {
    return new RegisterOlderAdultProfileCommand(
        resource.caregiverId(),
        resource.fullName(),
        resource.birthDate(),
        resource.emergencyContactName(),
        resource.emergencyContactRelationship(),
        resource.emergencyContactPhone());
  }
}
