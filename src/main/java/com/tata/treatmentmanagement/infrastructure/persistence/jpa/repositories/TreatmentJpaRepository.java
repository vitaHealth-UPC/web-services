package com.tata.treatmentmanagement.infrastructure.persistence.jpa.repositories;

import com.tata.treatmentmanagement.domain.model.valueobjects.TreatmentStatus;
import com.tata.treatmentmanagement.infrastructure.persistence.jpa.entities.TreatmentPersistenceEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TreatmentJpaRepository extends JpaRepository<TreatmentPersistenceEntity, String> {
    List<TreatmentPersistenceEntity> findByOlderAdultIdOrderByCreatedAtAsc(String olderAdultId);
    List<TreatmentPersistenceEntity> findByMedicationId(String medicationId);
    List<TreatmentPersistenceEntity> findByStatus(TreatmentStatus status);
}
