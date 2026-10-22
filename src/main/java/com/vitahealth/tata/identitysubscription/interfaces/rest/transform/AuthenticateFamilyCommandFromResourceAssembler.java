package com.vitahealth.tata.identitysubscription.interfaces.rest.transform;

import com.vitahealth.tata.identitysubscription.domain.model.commands.AuthenticateFamilyCommand;
import com.vitahealth.tata.identitysubscription.interfaces.rest.resources.SignInResource;

/** Translates the SignInResource HTTP contract into its application command. */
public final class AuthenticateFamilyCommandFromResourceAssembler {
  private AuthenticateFamilyCommandFromResourceAssembler() {}

  /** Preserves supplied input without applying domain policy or credential normalization. */
  public static AuthenticateFamilyCommand toCommand(SignInResource resource) {
    return new AuthenticateFamilyCommand(resource.email(), resource.password());
  }
}
