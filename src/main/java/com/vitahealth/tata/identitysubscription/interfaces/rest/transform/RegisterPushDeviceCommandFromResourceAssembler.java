package com.vitahealth.tata.identitysubscription.interfaces.rest.transform;

import com.vitahealth.tata.identitysubscription.domain.model.commands.RegisterPushDeviceCommand;
import com.vitahealth.tata.identitysubscription.interfaces.rest.resources.RegisterPushDeviceResource;

/** Translates device HTTP input without making ownership or session decisions. */
public final class RegisterPushDeviceCommandFromResourceAssembler {
  private RegisterPushDeviceCommandFromResourceAssembler() {}

  /** Copies the route identity and opaque address into the registration command. */
  public static RegisterPushDeviceCommand toCommand(
      String installationId, RegisterPushDeviceResource resource) {
    return new RegisterPushDeviceCommand(installationId, resource.token());
  }
}
