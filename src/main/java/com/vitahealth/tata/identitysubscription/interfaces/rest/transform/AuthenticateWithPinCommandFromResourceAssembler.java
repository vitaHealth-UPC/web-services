package com.vitahealth.tata.identitysubscription.interfaces.rest.transform;

import com.vitahealth.tata.identitysubscription.domain.model.commands.AuthenticateWithPinCommand;
import com.vitahealth.tata.identitysubscription.interfaces.rest.resources.PinSignInResource;

/** Translates adult sign-in input into the PIN authentication command. */
public final class AuthenticateWithPinCommandFromResourceAssembler {
  private AuthenticateWithPinCommandFromResourceAssembler() {}

  /** Preserves the credential without applying validation or hashing in the HTTP layer. */
  public static AuthenticateWithPinCommand toCommand(PinSignInResource resource) {
    return new AuthenticateWithPinCommand(resource.olderAdultId(), resource.pin());
  }
}
