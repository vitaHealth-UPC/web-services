package com.tata.treatmentmanagement.domain.model.commands;
public record RegisterMedicationCommand(String caregiverId, String olderAdultId, String name, String presentation) {}
