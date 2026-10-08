package com.tata.treatmentmanagement.infrastructure.persistence.jpa.adapters;

import com.tata.treatmentmanagement.domain.model.aggregates.Treatment;
import com.tata.treatmentmanagement.domain.model.valueobjects.TreatmentStatus;
import com.tata.treatmentmanagement.domain.repositories.TreatmentRepository;
import com.tata.treatmentmanagement.infrastructure.persistence.jpa.assemblers.TreatmentPersistenceAssembler;
import com.tata.treatmentmanagement.infrastructure.persistence.jpa.repositories.TreatmentJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class TreatmentRepositoryImpl implements TreatmentRepository {
    private final TreatmentJpaRepository repository;

    public TreatmentRepositoryImpl(TreatmentJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Treatment save(Treatment treatment) {
        return TreatmentPersistenceAssembler.toDomain(repository.save(TreatmentPersistenceAssembler.toEntity(treatment)));
    }

    @Override
    public Optional<Treatment> findById(String id) {
        return repository.findById(id).map(TreatmentPersistenceAssembler::toDomain);
    }

    @Override
    public List<Treatment> findByOlderAdultId(String olderAdultId) {
        return repository.findByOlderAdultIdOrderByCreatedAtAsc(olderAdultId).stream().map(TreatmentPersistenceAssembler::toDomain).toList();
    }

    @Override
    public List<Treatment> findByMedicationId(String medicationId) {
        return repository.findByMedicationId(medicationId).stream().map(TreatmentPersistenceAssembler::toDomain).toList();
    }

    @Override
    public List<Treatment> findByStatus(TreatmentStatus status) {
        return repository.findByStatus(status).stream().map(TreatmentPersistenceAssembler::toDomain).toList();
    }

}
