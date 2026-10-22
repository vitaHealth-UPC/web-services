package com.vitahealth.tata.treatmentmanagement.domain.model.queries;

/**
 * Requests the getTreatment projection for a caregiver and its explicit resource scope.
 *
 * @param caregiverId caregiver requesting current consented access
 * @param treatmentId identity delimiting the requested projection
 */
public record GetTreatmentQuery(String caregiverId, String treatmentId) {}
