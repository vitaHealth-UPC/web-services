package com.vitahealth.tata.inventoryreplenishment.domain.model.events;

import static com.vitahealth.tata.shared.domain.validation.DomainText.requireText;

import java.time.Instant;
import java.util.Objects;

/** Complete stock evidence published from a successfully flushed inventory snapshot. */
public record InventoryStockChanged(
    String inventoryId,
    String medicationId,
    int remainingStock,
    int replenishmentThreshold,
    long revision,
    Instant changedAt)
    implements InventoryEvent {
  /**
   * Accepts healthy and low stock while requiring valid identity, quantities and source revision.
   */
  public InventoryStockChanged {
    inventoryId = requireText(inventoryId, "inventoryId");
    medicationId = requireText(medicationId, "medicationId");
    Objects.requireNonNull(changedAt, "changedAt");
    if (remainingStock < 0 || replenishmentThreshold < 0 || revision < 0)
      throw new IllegalArgumentException("invalid inventory stock evidence");
  }
}
