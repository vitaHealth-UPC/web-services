package com.tata.treatmentmanagement.domain.repositories;

import com.tata.treatmentmanagement.domain.model.aggregates.Medication;
import java.util.Optional;

public interface MedicationRepository {
    Medication save(Medication medication);
    Optional<Medication> findById(String id);
}
