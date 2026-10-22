package com.vitahealth.tata.treatmentmanagement.domain.model.commands;

/** Restores availability; treatment resumption remains a separate explicit decision. */
public record ReactivateMedicationCommand(String caregiverId, String medicationId) {}
