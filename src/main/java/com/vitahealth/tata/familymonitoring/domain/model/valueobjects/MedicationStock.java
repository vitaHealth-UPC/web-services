package com.vitahealth.tata.familymonitoring.domain.model.valueobjects;

import static com.vitahealth.tata.shared.domain.validation.DomainText.requireText;

import java.util.Objects;
import java.util.Optional;

/** Complete local medication evidence, possibly without registered initial inventory. */
public record MedicationStock(String medicationName, Optional<InventoryStockPosition> stock) {
  /** Requires a named medication and an explicit present-or-absent stock result. */
  public MedicationStock {
    medicationName = requireText(medicationName, "medicationName");
    Objects.requireNonNull(stock, "stock");
  }
}
