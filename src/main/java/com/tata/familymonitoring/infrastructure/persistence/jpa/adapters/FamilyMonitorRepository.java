package com.tata.familymonitoring.infrastructure.persistence.jpa.adapters;

import com.tata.familymonitoring.infrastructure.persistence.jpa.repositories.FamilyMonitorJpaRepository;
import com.tata.familymonitoring.domain.model.aggregates.FamilyMonitor;
import com.tata.familymonitoring.domain.repositories.IFamilyMonitorRepository;
import java.util.Optional;
import com.tata.familymonitoring.infrastructure.persistence.jpa.assemblers.PersistenceAssembler;
import org.springframework.stereotype.Repository;

@Repository
@org.springframework.transaction.annotation.Transactional
public class FamilyMonitorRepository implements IFamilyMonitorRepository {

  private final FamilyMonitorJpaRepository jpaRepository;

  public FamilyMonitorRepository(FamilyMonitorJpaRepository jpaRepository) {
    this.jpaRepository = jpaRepository;
  }

  @Override
  public FamilyMonitor save(FamilyMonitor monitor) {
    return PersistenceAssembler.toDomain(jpaRepository.saveAndFlush(PersistenceAssembler.toEntity(monitor)));
  }

  @Override
  public Optional<FamilyMonitor> findByCareLinkId(String careLinkId) {
    return jpaRepository.findByCareLinkId(careLinkId).map(PersistenceAssembler::toDomain);
  }

  @Override
  public Optional<FamilyMonitor> findByOlderAdultId(String olderAdultId) {
    return jpaRepository.findByOlderAdultId(olderAdultId).map(PersistenceAssembler::toDomain);
  }
}
