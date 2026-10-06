package com.tata.familymonitoring.infrastructure.persistence.jpa;

import com.tata.familymonitoring.domain.model.aggregates.FamilyMonitor;
import com.tata.familymonitoring.domain.repositories.IFamilyMonitorRepository;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class FamilyMonitorRepository implements IFamilyMonitorRepository {

  private final FamilyMonitorJpaRepository jpaRepository;

  public FamilyMonitorRepository(FamilyMonitorJpaRepository jpaRepository) {
    this.jpaRepository = jpaRepository;
  }

  @Override
  public FamilyMonitor save(FamilyMonitor monitor) {
    return jpaRepository.saveAndFlush(monitor);
  }

  @Override
  public Optional<FamilyMonitor> findByCareLinkId(String careLinkId) {
    return jpaRepository.findByCareLinkId(careLinkId);
  }

  @Override
  public Optional<FamilyMonitor> findByOlderAdultId(String olderAdultId) {
    return jpaRepository.findByOlderAdultId(olderAdultId);
  }
}
