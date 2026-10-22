package com.vitahealth.tata.identitysubscription.interfaces.rest.transform;

import com.vitahealth.tata.identitysubscription.domain.model.commands.RegisterFamilyAccountCommand;
import com.vitahealth.tata.identitysubscription.interfaces.rest.resources.RegisterAccountResource;

/** Translates the RegisterAccountResource HTTP contract into its application command. */
public final class RegisterFamilyAccountCommandFromResourceAssembler {
  private RegisterFamilyAccountCommandFromResourceAssembler() {}

  /** Preserves supplied input without applying domain policy or credential normalization. */
  public static RegisterFamilyAccountCommand toCommand(RegisterAccountResource resource) {
    return new RegisterFamilyAccountCommand(resource.name(), resource.email(), resource.password());
  }
}
