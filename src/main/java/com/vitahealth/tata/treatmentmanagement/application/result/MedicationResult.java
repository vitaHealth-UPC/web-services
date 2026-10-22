package com.vitahealth.tata.treatmentmanagement.application.result;

/** Read projection of medication identity, ownership and current availability. */
public record MedicationResult(
    String id, String olderAdultId, String name, String presentation, boolean active) {}
