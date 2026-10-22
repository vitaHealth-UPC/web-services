package com.vitahealth.tata.familymonitoring.domain.model.valueobjects;

/** Medication ownership and name without importing another context's aggregate or storage. */
public record MedicationInfo(String olderAdultId, String name) {}
