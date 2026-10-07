package com.tata.intakeexecution.domain.model.valueobjects;

public record MedicationSnapshot(
        String name,
        String dose,
        String instructions
) {
    public MedicationSnapshot {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("medication name is required");
        if (dose == null || dose.isBlank()) throw new IllegalArgumentException("dose is required");
        name = name.trim();
        dose = dose.trim();
        instructions = instructions == null ? "" : instructions.trim();
    }
}
