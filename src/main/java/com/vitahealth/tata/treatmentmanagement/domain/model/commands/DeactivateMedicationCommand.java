package com.vitahealth.tata.treatmentmanagement.domain.model.commands;

/** Requests medication deactivation and coordinated suspension of its dependent treatments. */
public record DeactivateMedicationCommand(String caregiverId, String medicationId) {}
