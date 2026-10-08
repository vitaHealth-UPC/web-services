package com.tata.treatmentmanagement.infrastructure.persistence.jpa.adapters;

import com.tata.treatmentmanagement.domain.model.aggregates.Medication;
import com.tata.treatmentmanagement.domain.repositories.MedicationRepository;
import com.tata.treatmentmanagement.infrastructure.persistence.jpa.assemblers.MedicationPersistenceAssembler;
import com.tata.treatmentmanagement.infrastructure.persistence.jpa.repositories.MedicationJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class MedicationRepositoryImpl implements MedicationRepository {
    private final MedicationJpaRepository repository;

    public MedicationRepositoryImpl(MedicationJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Medication save(Medication medication) {
        return MedicationPersistenceAssembler.toDomain(repository.save(MedicationPersistenceAssembler.toEntity(medication)));
    }

    @Override
    public Optional<Medication> findById(String id) {
        return repository.findById(id).map(MedicationPersistenceAssembler::toDomain);
    }

    @Override
    public List<Medication> findByOlderAdultId(String olderAdultId) {
        return repository.findByOlderAdultIdOrderByNameAsc(olderAdultId).stream().map(MedicationPersistenceAssembler::toDomain).toList();
    }

}
