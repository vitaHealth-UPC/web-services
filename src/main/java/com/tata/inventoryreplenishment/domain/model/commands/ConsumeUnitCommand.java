package com.tata.inventoryreplenishment.domain.model.commands;

/** Internal command: one confirmed intake consumes one unit of its medication. */
public record ConsumeUnitCommand(String medicationId, String intakeId) {
    public ConsumeUnitCommand {
        if (medicationId == null || medicationId.isBlank()) {
            throw new IllegalArgumentException("medicationId is required");
        }
        if (intakeId == null || intakeId.isBlank()) {
            throw new IllegalArgumentException("intakeId is required");
        }
        medicationId = medicationId.trim();
        intakeId = intakeId.trim();
    }
}
