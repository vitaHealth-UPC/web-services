package com.tata.treatmentmanagement.application.internal;

import com.tata.treatmentmanagement.application.models.MedicationResult;
import com.tata.treatmentmanagement.application.models.TreatmentResult;
import com.tata.treatmentmanagement.domain.model.aggregates.Medication;
import com.tata.treatmentmanagement.domain.model.aggregates.Treatment;

public final class TreatmentMapper {
    private TreatmentMapper() {}

    public static MedicationResult toResult(Medication medication) {
        return new MedicationResult(
                medication.id(), medication.olderAdultId(), medication.name(),
                medication.presentation(), medication.active()
        );
    }

    public static TreatmentResult toResult(Treatment treatment) {
        var regimen = treatment.regimen();
        return new TreatmentResult(
                treatment.id(),
                treatment.olderAdultId(),
                treatment.name(),
                treatment.status(),
                regimen == null ? null : regimen.medicationId(),
                regimen == null ? null : regimen.dose(),
                regimen == null ? null : regimen.frequency(),
                regimen == null ? null : regimen.scheduledTimes(),
                regimen == null ? null : regimen.instructions(),
                regimen == null ? null : regimen.reminderLeadMinutes()
        );
    }
}
