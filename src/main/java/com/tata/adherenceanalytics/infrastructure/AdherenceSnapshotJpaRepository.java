package com.tata.adherenceanalytics.infrastructure;

import com.tata.adherenceanalytics.domain.model.AdherencePeriodSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdherenceSnapshotJpaRepository extends JpaRepository<AdherencePeriodSnapshot, String> {}
