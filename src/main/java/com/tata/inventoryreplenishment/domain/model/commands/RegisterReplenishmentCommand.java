package com.tata.inventoryreplenishment.domain.model.commands;

public record RegisterReplenishmentCommand(String medicationId, int quantity) {
    public RegisterReplenishmentCommand {
        if (medicationId == null || medicationId.isBlank()) {
            throw new IllegalArgumentException("medicationId is required");
        }
        medicationId = medicationId.trim();
    }
}
