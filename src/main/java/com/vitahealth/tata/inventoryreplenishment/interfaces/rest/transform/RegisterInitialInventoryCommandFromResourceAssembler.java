package com.vitahealth.tata.inventoryreplenishment.interfaces.rest.transform;

import com.vitahealth.tata.inventoryreplenishment.domain.model.commands.RegisterInitialInventoryCommand;
import com.vitahealth.tata.inventoryreplenishment.interfaces.rest.resources.RegisterInitialInventoryResource;

/** Translates validated transport input into its specific stock mutation command. */
public final class RegisterInitialInventoryCommandFromResourceAssembler {
  private RegisterInitialInventoryCommandFromResourceAssembler() {}

  /** Copies request evidence without accessing persistence or computing stock. */
  public static RegisterInitialInventoryCommand fromResource(
      RegisterInitialInventoryResource resource) {
    return new RegisterInitialInventoryCommand(
        resource.medicationId(), resource.initialQuantity(), resource.replenishmentThreshold());
  }
}
