package com.tata.treatmentmanagement.interfaces.acl;

import com.tata.treatmentmanagement.application.models.MedicationResult;
import com.tata.treatmentmanagement.application.models.TreatmentResult;
import java.util.List;
import java.util.Optional;

public interface TreatmentContextFacade {
    MedicationResult getMedication(String caregiverId, String medicationId);
    TreatmentResult getTreatment(String caregiverId, String treatmentId);
    List<MedicationResult> listMedications(String caregiverId, String olderAdultId);
    List<TreatmentResult> listTreatments(String caregiverId, String olderAdultId);

    /**
     * Contract for other modules that react to events carrying only a medication id (low stock, for
     * example). It does no care-link check because there is no caregiver involved.
     */
    Optional<MedicationResult> findMedication(String medicationId);
    Optional<TreatmentResult> findTreatment(String treatmentId);
    Optional<Integer> scheduledDailyUnits(String medicationId);
}
