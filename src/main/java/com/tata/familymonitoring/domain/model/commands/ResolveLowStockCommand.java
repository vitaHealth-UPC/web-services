package com.tata.familymonitoring.domain.model.commands;

public record ResolveLowStockCommand(String medicationId, int remainingStock) {
}
