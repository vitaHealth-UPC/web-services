package com.vitahealth.tata.treatmentmanagement.domain.model.commands;

/** Registers medication identity and descriptive data independently of a treatment regimen. */
public record RegisterMedicationCommand(
    String caregiverId, String olderAdultId, String name, String presentation) {}
