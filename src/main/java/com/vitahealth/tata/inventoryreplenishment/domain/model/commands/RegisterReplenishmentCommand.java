package com.vitahealth.tata.inventoryreplenishment.domain.model.commands;

/** Adds one stock receipt, optionally retaining a lot reference supplied by the caregiver. */
public record RegisterReplenishmentCommand(String medicationId, int quantity, String lot) {
  /** Creates a receipt command without a lot reference. */
  public RegisterReplenishmentCommand(String medicationId, int quantity) {
    this(medicationId, quantity, null);
  }

  /** Requires a source medication and bounds optional lot text before aggregate mutation. */
  public RegisterReplenishmentCommand {
    if (medicationId == null || medicationId.isBlank()) {
      throw new IllegalArgumentException("medicationId is required");
    }
    medicationId = medicationId.trim();
    lot = lot == null || lot.isBlank() ? null : lot.trim();
    if (lot != null && lot.length() > 200)
      throw new IllegalArgumentException("lot must not exceed 200 characters");
  }
}
