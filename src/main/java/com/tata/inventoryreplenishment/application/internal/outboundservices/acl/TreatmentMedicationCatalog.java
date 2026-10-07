package com.tata.inventoryreplenishment.application.internal.outboundservices.acl;

import com.tata.inventoryreplenishment.application.internal.outboundservices.MedicationCatalog;
import com.tata.treatmentmanagement.interfaces.acl.TreatmentContextFacade;
import org.springframework.stereotype.Component;

@Component
public class TreatmentMedicationCatalog implements MedicationCatalog {
    private final TreatmentContextFacade treatments;

    public TreatmentMedicationCatalog(TreatmentContextFacade treatments) {
        this.treatments = treatments;
    }

    @Override
    public Availability availability(String medicationId) {
        return treatments.findMedication(medicationId)
                .map(medication -> medication.active() ? Availability.ACTIVE : Availability.INACTIVE)
                .orElse(Availability.MISSING);
    }
}
