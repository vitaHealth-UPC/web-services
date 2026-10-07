package com.tata.treatmentmanagement.application.internal.fakes;

import com.tata.treatmentmanagement.domain.model.aggregates.Medication;
import com.tata.treatmentmanagement.domain.repositories.MedicationRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class InMemoryMedicationRepository implements MedicationRepository {
    private final Map<String, Medication> values = new HashMap<>();

    @Override
    public Medication save(Medication medication) {
        values.put(medication.id(), medication);
        return medication;
    }

    @Override
    public Optional<Medication> findById(String id) {
        return Optional.ofNullable(values.get(id));
    }

    @Override
    public List<Medication> findByOlderAdultId(String olderAdultId) {
        return values.values().stream()
                .filter(medication -> medication.olderAdultId().equals(olderAdultId))
                .toList();
    }
}
