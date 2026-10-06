package com.tata.treatmentmanagement.domain.repositories;

import com.tata.treatmentmanagement.domain.model.aggregates.Treatment;
import java.util.Optional;

public interface TreatmentRepository {
    Treatment save(Treatment treatment);
    Optional<Treatment> findById(String id);
}
