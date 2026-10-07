package com.tata.treatmentmanagement.infrastructure.persistence.jpa.adapters;

import com.tata.treatmentmanagement.domain.model.aggregates.Medication;
import com.tata.treatmentmanagement.domain.repositories.MedicationRepository;
import com.tata.treatmentmanagement.infrastructure.persistence.jpa.entities.MedicationPersistenceEntity;
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
        return toDomain(repository.save(toEntity(medication)));
    }

    @Override
    public Optional<Medication> findById(String id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    public List<Medication> findByOlderAdultId(String olderAdultId) {
        return repository.findByOlderAdultIdOrderByNameAsc(olderAdultId).stream().map(this::toDomain).toList();
    }

    private Medication toDomain(MedicationPersistenceEntity entity) {
        return Medication.rehydrate(
                entity.getId(), entity.getOlderAdultId(), entity.getName(), entity.getPresentation(),
                entity.isActive(), entity.getCreatedAt()
        );
    }

    private MedicationPersistenceEntity toEntity(Medication medication) {
        return new MedicationPersistenceEntity(
                medication.id(), medication.olderAdultId(), medication.name(), medication.presentation(),
                medication.active(), medication.createdAt()
        );
    }
}
