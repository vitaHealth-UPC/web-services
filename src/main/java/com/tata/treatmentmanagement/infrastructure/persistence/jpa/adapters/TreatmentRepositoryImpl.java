package com.tata.treatmentmanagement.infrastructure.persistence.jpa.adapters;

import com.tata.treatmentmanagement.domain.model.aggregates.Treatment;
import com.tata.treatmentmanagement.domain.model.valueobjects.TreatmentRegimen;
import com.tata.treatmentmanagement.domain.repositories.TreatmentRepository;
import com.tata.treatmentmanagement.infrastructure.persistence.jpa.entities.TreatmentPersistenceEntity;
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
        return toDomain(repository.save(toEntity(treatment)));
    }

    @Override
    public Optional<Treatment> findById(String id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    public List<Treatment> findByOlderAdultId(String olderAdultId) {
        return repository.findByOlderAdultIdOrderByCreatedAtAsc(olderAdultId).stream().map(this::toDomain).toList();
    }

    @Override
    public List<Treatment> findByMedicationId(String medicationId) {
        return repository.findByMedicationId(medicationId).stream().map(this::toDomain).toList();
    }

    private Treatment toDomain(TreatmentPersistenceEntity entity) {
        TreatmentRegimen regimen = null;
        if (entity.getMedicationId() != null) {
            regimen = new TreatmentRegimen(
                    entity.getMedicationId(),
                    entity.getDose(),
                    entity.getFrequency(),
                    entity.getScheduledTimes(),
                    entity.getInstructions(),
                    entity.getReminderLeadMinutes() == null ? 0 : entity.getReminderLeadMinutes()
            );
        }
        return Treatment.rehydrate(
                entity.getId(), entity.getOlderAdultId(), entity.getName(), entity.getStatus(),
                regimen, entity.getCreatedAt()
        );
    }

    private TreatmentPersistenceEntity toEntity(Treatment treatment) {
        var regimen = treatment.regimen();
        return new TreatmentPersistenceEntity(
                treatment.id(),
                treatment.olderAdultId(),
                treatment.name(),
                treatment.status(),
                regimen == null ? null : regimen.medicationId(),
                regimen == null ? null : regimen.dose(),
                regimen == null ? null : regimen.frequency(),
                regimen == null ? null : regimen.scheduledTimes(),
                regimen == null ? null : regimen.instructions(),
                regimen == null ? null : regimen.reminderLeadMinutes(),
                treatment.createdAt()
        );
    }
}
