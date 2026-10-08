package com.tata.adherenceanalytics.domain.repositories;

import com.tata.adherenceanalytics.domain.model.AdherencePeriodSnapshot;
import java.util.Optional;

public interface AdherenceSnapshotRepository {
    Optional<AdherencePeriodSnapshot> find(String id);
    AdherencePeriodSnapshot saveIfAbsent(AdherencePeriodSnapshot snapshot);
}
