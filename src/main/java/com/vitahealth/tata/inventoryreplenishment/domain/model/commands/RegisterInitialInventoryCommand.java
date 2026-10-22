package com.vitahealth.tata.inventoryreplenishment.domain.model.commands;

/** Registers initial stock and its replenishment threshold for an existing active medication. */
public record RegisterInitialInventoryCommand(
    String medicationId, int initialQuantity, int replenishmentThreshold) {
  /** Requires a medication reference; aggregate creation validates stock quantities. */
  public RegisterInitialInventoryCommand {
    if (medicationId == null || medicationId.isBlank()) {
      throw new IllegalArgumentException("medicationId is required");
    }
    medicationId = medicationId.trim();
  }
}
