package com.vitahealth.tata.treatmentmanagement.domain.model.queries;

/**
 * Requests the getMedication projection for a caregiver and its explicit resource scope.
 *
 * @param caregiverId caregiver requesting current consented access
 * @param medicationId identity delimiting the requested projection
 */
public record GetMedicationQuery(String caregiverId, String medicationId) {}
