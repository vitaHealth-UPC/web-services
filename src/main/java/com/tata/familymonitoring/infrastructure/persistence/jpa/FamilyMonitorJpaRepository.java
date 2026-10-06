package com.tata.familymonitoring.infrastructure.persistence.jpa;

import com.tata.familymonitoring.domain.model.aggregates.FamilyMonitor;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FamilyMonitorJpaRepository extends JpaRepository<FamilyMonitor, Long> {

  Optional<FamilyMonitor> findByCareLinkId(String careLinkId);

  Optional<FamilyMonitor> findByOlderAdultId(String olderAdultId);
}
