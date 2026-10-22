package com.vitahealth.tata.familymonitoring.domain.model.commands;

import com.vitahealth.tata.familymonitoring.domain.model.valueobjects.InventoryStockPosition;
import java.util.Objects;

/** Projects one complete inventory snapshot through the medication owner's consented follow-ups. */
public record ProjectInventoryStockCommand(InventoryStockPosition position) {
  /** Requires validated immutable source evidence before selecting any relationship. */
  public ProjectInventoryStockCommand {
    Objects.requireNonNull(position, "position");
  }
}
