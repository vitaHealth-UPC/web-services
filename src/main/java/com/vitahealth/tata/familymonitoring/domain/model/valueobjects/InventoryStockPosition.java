package com.vitahealth.tata.familymonitoring.domain.model.valueobjects;

import static com.vitahealth.tata.shared.domain.validation.DomainText.requireText;

import java.time.Instant;
import java.util.Objects;

/** Last complete inventory evidence, retained even when the medication has healthy stock. */
public record InventoryStockPosition(
    String medicationId,
    String inventoryId,
    long revision,
    int remainingStock,
    int replenishmentThreshold,
    Instant changedAt) {

  /** Requires source identity and nonnegative stock and revision evidence. */
  public InventoryStockPosition {
    medicationId = requireText(medicationId, "medicationId");
    inventoryId = requireText(inventoryId, "inventoryId");
    Objects.requireNonNull(changedAt, "changedAt");
    if (revision < 0 || remainingStock < 0 || replenishmentThreshold < 0) {
      throw new IllegalArgumentException("inventory evidence must be nonnegative");
    }
  }

  /**
   * Returns whether this snapshot advances the previously observed source revision. Exact replays
   * and older revisions do not change the projection; conflicting source evidence fails explicitly.
   * Source timestamps are evidence, while revisions determine ordering.
   */
  public boolean advances(InventoryStockPosition previous) {
    Objects.requireNonNull(previous, "previous");
    if (!medicationId.equals(previous.medicationId) || !inventoryId.equals(previous.inventoryId)) {
      throw new IllegalStateException("inventory source identity conflicts with stored evidence");
    }
    if (revision < previous.revision) return false;
    if (revision == previous.revision) {
      if (!equals(previous)) {
        throw new IllegalStateException("inventory revision contains conflicting stock evidence");
      }
      return false;
    }
    return true;
  }

  /** Reports whether the observed quantity requires a replenishment notice. */
  public boolean isLowStock() {
    return remainingStock <= replenishmentThreshold;
  }
}
