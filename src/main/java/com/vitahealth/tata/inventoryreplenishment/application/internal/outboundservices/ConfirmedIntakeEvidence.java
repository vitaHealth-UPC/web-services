package com.vitahealth.tata.inventoryreplenishment.application.internal.outboundservices;

import com.vitahealth.tata.inventoryreplenishment.domain.model.commands.RequestInventoryConsumptionCommand;

/** Verifies original source evidence before Inventory reserves or changes decrement intent. */
public interface ConfirmedIntakeEvidence {
  /**
   * Locks the source until the caller transaction ends and checks its identity, owner, medication
   * and exact definitive confirmation instant. Returns false for absent or mismatched evidence.
   */
  boolean lockAndVerify(RequestInventoryConsumptionCommand command);
}
