package com.tata.inventoryreplenishment.domain.model.commands;

public record RegisterReplenishmentCommand(String medicationId, int quantity, String lot) {
    public RegisterReplenishmentCommand(String medicationId, int quantity) { this(medicationId, quantity, null); }
    public RegisterReplenishmentCommand {
        if (medicationId == null || medicationId.isBlank()) {
            throw new IllegalArgumentException("medicationId is required");
        }
        medicationId = medicationId.trim();
        lot = lot == null || lot.isBlank() ? null : lot.trim();
        if (lot != null && lot.length() > 200) throw new IllegalArgumentException("lot must not exceed 200 characters");
    }
}
