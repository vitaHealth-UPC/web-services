package com.vitahealth.tata.inventoryreplenishment.domain.model.queries;

/** Selects inventory by its logical medication reference. */
public record GetRemainingStockQuery(String medicationId) {
  /** Rejects missing selectors and normalizes surrounding input whitespace. */
  public GetRemainingStockQuery {
    if (medicationId == null || medicationId.isBlank()) {
      throw new IllegalArgumentException("medicationId is required");
    }
    medicationId = medicationId.trim();
  }
}
