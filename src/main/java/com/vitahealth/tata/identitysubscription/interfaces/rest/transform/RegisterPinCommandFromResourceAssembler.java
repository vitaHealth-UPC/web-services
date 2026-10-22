package com.vitahealth.tata.identitysubscription.interfaces.rest.transform;

import com.vitahealth.tata.identitysubscription.domain.model.commands.RegisterPinCommand;
import com.vitahealth.tata.identitysubscription.interfaces.rest.resources.RegisterPinResource;

/** Translates authorized PIN enrollment input into its application command. */
public final class RegisterPinCommandFromResourceAssembler {
  private RegisterPinCommandFromResourceAssembler() {}

  /** Preserves adult identity and the exact PIN supplied by the client. */
  public static RegisterPinCommand toCommand(RegisterPinResource resource) {
    return new RegisterPinCommand(resource.olderAdultId(), resource.pin());
  }
}
