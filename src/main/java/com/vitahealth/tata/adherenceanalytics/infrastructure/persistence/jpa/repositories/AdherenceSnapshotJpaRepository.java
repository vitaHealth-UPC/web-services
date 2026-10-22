package com.vitahealth.tata.adherenceanalytics.infrastructure.persistence.jpa.repositories;

import com.vitahealth.tata.adherenceanalytics.infrastructure.persistence.jpa.entities.AdherenceSnapshotPersistenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/** Stores immutable snapshot evidence under its deterministic owner-period-zone identity. */
public interface AdherenceSnapshotJpaRepository
    extends JpaRepository<AdherenceSnapshotPersistenceEntity, String> {}
