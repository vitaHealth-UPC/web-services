package com.tata.inventoryreplenishment.domain.model.commands;

public record RegisterInitialInventoryCommand(
        String medicationId,
        int initialQuantity,
        int replenishmentThreshold
) {
    public RegisterInitialInventoryCommand {
        if (medicationId == null || medicationId.isBlank()) {
            throw new IllegalArgumentException("medicationId is required");
        }
        medicationId = medicationId.trim();
    }
}
