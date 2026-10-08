package com.tata.treatmentmanagement.interfaces.rest.transform;

import com.tata.treatmentmanagement.application.models.MedicationResult;
import com.tata.treatmentmanagement.application.models.MedicationCatalogItem;
import com.tata.treatmentmanagement.interfaces.rest.resources.MedicationCatalogResource;
import com.tata.treatmentmanagement.application.models.TreatmentResult;
import com.tata.treatmentmanagement.interfaces.rest.resources.MedicationResources.MedicationResponse;
import com.tata.treatmentmanagement.interfaces.rest.resources.TreatmentResources.TreatmentResponse;

public final class TreatmentResourceAssembler {
    private TreatmentResourceAssembler() {}

    public static MedicationResponse toResource(MedicationResult result) {
        return new MedicationResponse(
                result.id(), result.olderAdultId(), result.name(), result.presentation(), result.active()
        );
    }

    public static TreatmentResponse toResource(TreatmentResult result) {
        return new TreatmentResponse(
                result.id(), result.olderAdultId(), result.name(), result.status().name(),
                result.medicationId(), result.dose(), result.frequency(), result.scheduledTimes(),
                result.instructions(), result.reminderLeadMinutes()
        );
    }
    public static MedicationCatalogResource toResource(MedicationCatalogItem result) {
        return new MedicationCatalogResource(toResource(result.medication()),
                result.treatments().stream().map(TreatmentResourceAssembler::toResource).toList());
    }
}
