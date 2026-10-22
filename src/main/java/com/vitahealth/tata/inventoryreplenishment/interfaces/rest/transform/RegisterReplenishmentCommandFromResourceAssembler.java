package com.vitahealth.tata.inventoryreplenishment.interfaces.rest.transform;

import com.vitahealth.tata.inventoryreplenishment.domain.model.commands.RegisterReplenishmentCommand;
import com.vitahealth.tata.inventoryreplenishment.interfaces.rest.resources.RegisterReplenishmentResource;

/** Translates validated transport input into its specific stock mutation command. */
public final class RegisterReplenishmentCommandFromResourceAssembler {
  private RegisterReplenishmentCommandFromResourceAssembler() {}

  /** Copies request evidence without accessing persistence or computing stock. */
  public static RegisterReplenishmentCommand fromResource(
      String medicationId, RegisterReplenishmentResource resource) {
    return new RegisterReplenishmentCommand(medicationId, resource.quantity(), resource.lot());
  }
}
