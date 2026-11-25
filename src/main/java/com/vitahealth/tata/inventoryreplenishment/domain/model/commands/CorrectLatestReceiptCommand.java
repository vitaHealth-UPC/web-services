package com.vitahealth.tata.inventoryreplenishment.domain.model.commands;

import static com.vitahealth.tata.shared.domain.validation.DomainText.requireText;

/**
 * Corrects the newest receipt of a medication's stock; {@code replenishmentThreshold} is only
 * accepted while that receipt is the initial stock.
 */
public record CorrectLatestReceiptCommand(
    String medicationId,
    String batchId,
    int quantity,
    String lot,
    Integer replenishmentThreshold) {
  /** Requires the stock and receipt identities. */
  public CorrectLatestReceiptCommand {
    medicationId = requireText(medicationId, "medicationId");
    batchId = requireText(batchId, "batchId");
  }
}
