package com.tata.treatmentmanagement.domain.model.commands;
public record UpdateMedicationCommand(
        String caregiverId,
        String medicationId,
        String name,
        String presentation
) {}
