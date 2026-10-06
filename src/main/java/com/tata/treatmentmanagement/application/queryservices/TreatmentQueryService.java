package com.tata.treatmentmanagement.application.queryservices;

import com.tata.treatmentmanagement.application.models.MedicationResult;
import com.tata.treatmentmanagement.application.models.TreatmentResult;

public interface TreatmentQueryService {
    MedicationResult getMedication(String caregiverId, String medicationId);
    TreatmentResult getTreatment(String caregiverId, String treatmentId);
}
