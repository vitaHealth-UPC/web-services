package com.vitahealth.tata.inventoryreplenishment.domain.model.commands;

import static com.vitahealth.tata.shared.domain.validation.DomainText.requireText;

import java.time.Instant;
import java.util.Objects;

/** Requests one inventory decrement using original committed intake confirmation evidence. */
public record RequestInventoryConsumptionCommand(
    String intakeId, String medicationId, String olderAdultId, Instant confirmedAt) {
  /** Validates logical references without changing the original confirmation instant. */
  public RequestInventoryConsumptionCommand {
    intakeId = requireText(intakeId, "intakeId");
    medicationId = requireText(medicationId, "medicationId");
    olderAdultId = requireText(olderAdultId, "olderAdultId");
    Objects.requireNonNull(confirmedAt, "confirmedAt");
  }
}
