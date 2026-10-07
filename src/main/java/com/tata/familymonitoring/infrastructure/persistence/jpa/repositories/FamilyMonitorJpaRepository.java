package com.tata.familymonitoring.infrastructure.persistence.jpa.repositories;

import com.tata.familymonitoring.infrastructure.persistence.jpa.entities.FamilyMonitorPersistenceEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FamilyMonitorJpaRepository extends JpaRepository<FamilyMonitorPersistenceEntity, Long> {

  Optional<FamilyMonitorPersistenceEntity> findByCareLinkId(String careLinkId);

  Optional<FamilyMonitorPersistenceEntity> findByOlderAdultId(String olderAdultId);
}
