package com.tata.treatmentmanagement.infrastructure.persistence.jpa.repositories;

import com.tata.treatmentmanagement.infrastructure.persistence.jpa.entities.TreatmentPersistenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TreatmentJpaRepository extends JpaRepository<TreatmentPersistenceEntity, String> {}
