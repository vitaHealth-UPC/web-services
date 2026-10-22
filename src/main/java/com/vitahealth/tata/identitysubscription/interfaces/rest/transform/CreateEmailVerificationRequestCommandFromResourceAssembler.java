package com.vitahealth.tata.identitysubscription.interfaces.rest.transform;

import com.vitahealth.tata.identitysubscription.domain.model.commands.RequestNewVerificationCommand;
import com.vitahealth.tata.identitysubscription.interfaces.rest.resources.CreateEmailVerificationRequestResource;

/** Maps a verification resend request without generating or revealing a verification code. */
public final class CreateEmailVerificationRequestCommandFromResourceAssembler {
  private CreateEmailVerificationRequestCommandFromResourceAssembler() {}

  /** Preserves the supplied email for the application verification workflow. */
  public static RequestNewVerificationCommand toCommandFromResource(
      CreateEmailVerificationRequestResource resource) {
    return new RequestNewVerificationCommand(resource.email());
  }
}
