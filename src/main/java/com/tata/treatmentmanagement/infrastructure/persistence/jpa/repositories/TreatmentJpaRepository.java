package com.tata.treatmentmanagement.infrastructure.persistence.jpa.repositories;

import com.tata.treatmentmanagement.infrastructure.persistence.jpa.entities.TreatmentPersistenceEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TreatmentJpaRepository extends JpaRepository<TreatmentPersistenceEntity, String> {
    List<TreatmentPersistenceEntity> findByOlderAdultIdOrderByCreatedAtAsc(String olderAdultId);
    List<TreatmentPersistenceEntity> findByMedicationId(String medicationId);
}
