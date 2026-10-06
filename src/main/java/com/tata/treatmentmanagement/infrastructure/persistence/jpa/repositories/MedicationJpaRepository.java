package com.tata.treatmentmanagement.infrastructure.persistence.jpa.repositories;

import com.tata.treatmentmanagement.infrastructure.persistence.jpa.entities.MedicationPersistenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MedicationJpaRepository extends JpaRepository<MedicationPersistenceEntity, String> {}
