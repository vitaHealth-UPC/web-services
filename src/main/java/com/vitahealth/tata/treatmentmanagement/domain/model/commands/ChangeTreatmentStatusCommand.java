package com.vitahealth.tata.treatmentmanagement.domain.model.commands;

/** Requests a lifecycle transition for a treatment under the supplied caregiver identity. */
public record ChangeTreatmentStatusCommand(String caregiverId, String treatmentId) {}
