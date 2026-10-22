package com.vitahealth.tata.inventoryreplenishment.application.result;

import java.time.Instant;

/** Immutable complete source snapshot; carries no inventory aggregate or persistence reference. */
public record InventoryStockSnapshot(
    String inventoryId,
    String medicationId,
    int remainingStock,
    int replenishmentThreshold,
    long revision,
    Instant changedAt) {}
