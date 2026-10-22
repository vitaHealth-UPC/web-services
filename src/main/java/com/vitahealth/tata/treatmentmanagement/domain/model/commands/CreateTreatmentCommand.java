package com.vitahealth.tata.treatmentmanagement.domain.model.commands;

/** Requests an unconfigured treatment draft for an adult owned through a consented care link. */
public record CreateTreatmentCommand(String caregiverId, String olderAdultId, String name) {}
