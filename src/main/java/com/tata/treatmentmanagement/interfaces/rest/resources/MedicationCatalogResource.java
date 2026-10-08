package com.tata.treatmentmanagement.interfaces.rest.resources;

import java.util.List;
import io.swagger.v3.oas.annotations.media.Schema;
import com.tata.treatmentmanagement.interfaces.rest.resources.MedicationResources.MedicationResponse;
import com.tata.treatmentmanagement.interfaces.rest.resources.TreatmentResources.TreatmentResponse;

@Schema(description = "Medication and associated treatments of the authenticated older adult")
public record MedicationCatalogResource(MedicationResponse medication, List<TreatmentResponse> treatments) {
    public MedicationCatalogResource {
        treatments = List.copyOf(treatments);
    }
}
