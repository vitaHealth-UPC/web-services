package com.tata.treatmentmanagement.application.internal.fakes;

import com.tata.treatmentmanagement.domain.model.aggregates.Treatment;
import com.tata.treatmentmanagement.domain.repositories.TreatmentRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class InMemoryTreatmentRepository implements TreatmentRepository {
    private final Map<String, Treatment> values = new HashMap<>();

    @Override
    public Treatment save(Treatment treatment) {
        values.put(treatment.id(), treatment);
        return treatment;
    }

    @Override
    public Optional<Treatment> findById(String id) {
        return Optional.ofNullable(values.get(id));
    }

    @Override
    public List<Treatment> findByOlderAdultId(String olderAdultId) {
        return values.values().stream()
                .filter(treatment -> treatment.olderAdultId().equals(olderAdultId))
                .toList();
    }

    @Override
    public List<Treatment> findByMedicationId(String medicationId) {
        return values.values().stream()
                .filter(treatment -> treatment.regimen() != null
                        && treatment.regimen().medicationId().equals(medicationId))
                .toList();
    }
}
