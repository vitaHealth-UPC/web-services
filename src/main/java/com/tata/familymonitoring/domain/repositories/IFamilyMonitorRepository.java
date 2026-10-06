package com.tata.familymonitoring.domain.repositories;

import com.tata.familymonitoring.domain.model.aggregates.FamilyMonitor;
import java.util.Optional;

public interface IFamilyMonitorRepository {

  FamilyMonitor save(FamilyMonitor monitor);

  Optional<FamilyMonitor> findByCareLinkId(String careLinkId);

  Optional<FamilyMonitor> findByOlderAdultId(String olderAdultId);
}
