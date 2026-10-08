package com.tata.treatmentmanagement.application.models;

import java.util.List;

public record MedicationCatalogItem(MedicationResult medication, List<TreatmentResult> treatments) {
    public MedicationCatalogItem {
        treatments = List.copyOf(treatments);
    }
}
