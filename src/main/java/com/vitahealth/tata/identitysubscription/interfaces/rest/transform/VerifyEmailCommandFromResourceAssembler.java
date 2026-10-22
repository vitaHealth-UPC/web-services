package com.vitahealth.tata.identitysubscription.interfaces.rest.transform;

import com.vitahealth.tata.identitysubscription.domain.model.commands.VerifyEmailCommand;
import com.vitahealth.tata.identitysubscription.interfaces.rest.resources.VerifyEmailResource;

/** Translates the VerifyEmailResource HTTP contract into its application command. */
public final class VerifyEmailCommandFromResourceAssembler {
  private VerifyEmailCommandFromResourceAssembler() {}

  /** Preserves supplied input without applying domain policy or credential normalization. */
  public static VerifyEmailCommand toCommand(VerifyEmailResource resource) {
    return new VerifyEmailCommand(resource.email(), resource.code());
  }
}
