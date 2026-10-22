package com.vitahealth.tata.treatmentmanagement.domain.model.commands;

/** Requests replacement medication descriptions while retaining the medication identity. */
public record UpdateMedicationCommand(
    String caregiverId, String medicationId, String name, String presentation) {}
