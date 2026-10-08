package com.tata.adherenceanalytics.infrastructure.persistence.jpa.repositories;

import com.tata.adherenceanalytics.infrastructure.persistence.jpa.entities.AdherenceSnapshotPersistenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdherenceSnapshotJpaRepository extends JpaRepository<AdherenceSnapshotPersistenceEntity, String> {}
