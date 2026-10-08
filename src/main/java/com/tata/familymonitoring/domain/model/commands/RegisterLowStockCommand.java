package com.tata.familymonitoring.domain.model.commands;

import java.time.Instant;

public record RegisterLowStockCommand(
    String medicationId,
    int remainingStock,
    int replenishmentThreshold,
    Instant detectedAt) {
}
