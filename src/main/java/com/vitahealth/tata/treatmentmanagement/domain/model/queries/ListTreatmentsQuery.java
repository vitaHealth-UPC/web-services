package com.vitahealth.tata.treatmentmanagement.domain.model.queries;

/**
 * Requests the listTreatments projection for a caregiver and its explicit resource scope.
 *
 * @param caregiverId caregiver requesting current consented access
 * @param olderAdultId identity delimiting the requested projection
 */
public record ListTreatmentsQuery(String caregiverId, String olderAdultId) {}
