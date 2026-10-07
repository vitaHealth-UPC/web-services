package com.tata.inventoryreplenishment.infrastructure.modules;

import com.tata.inventoryreplenishment.application.internal.outboundservices.MedicationCatalog;
import com.tata.treatmentmanagement.application.queryservices.TreatmentQueryService;
import org.springframework.stereotype.Component;

@Component
public class TreatmentMedicationCatalog implements MedicationCatalog {
    private final TreatmentQueryService treatments;

    public TreatmentMedicationCatalog(TreatmentQueryService treatments) {
        this.treatments = treatments;
    }

    @Override
    public Availability availability(String medicationId) {
        return treatments.findMedication(medicationId)
                .map(medication -> medication.active() ? Availability.ACTIVE : Availability.INACTIVE)
                .orElse(Availability.MISSING);
    }
}
