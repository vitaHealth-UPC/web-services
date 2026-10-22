package com.vitahealth.tata.familymonitoring.infrastructure.persistence.jpa.adapters;

import com.vitahealth.tata.familymonitoring.domain.model.aggregates.FamilyMonitor;
import com.vitahealth.tata.familymonitoring.domain.repositories.IFamilyMonitorRepository;
import com.vitahealth.tata.familymonitoring.infrastructure.persistence.jpa.assemblers.FamilyMonitorPersistenceAssembler;
import com.vitahealth.tata.familymonitoring.infrastructure.persistence.jpa.assemblers.FamilyMonitorPersistenceStateUpdater;
import com.vitahealth.tata.familymonitoring.infrastructure.persistence.jpa.repositories.FamilyMonitorJpaRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.OptimisticLockException;
import java.time.Clock;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Stores follow-up snapshots through reserved managed state and explicit revision checks. */
@Repository
@Transactional
public class FamilyMonitorRepository implements IFamilyMonitorRepository {

  private final FamilyMonitorJpaRepository jpaRepository;
  private final EntityManager entities;
  private final Clock clock;

  /** Injects storage, the reservation persistence context and the shared audit clock. */
  public FamilyMonitorRepository(
      FamilyMonitorJpaRepository jpaRepository, EntityManager entities, Clock clock) {
    this.jpaRepository = jpaRepository;
    this.entities = entities;
    this.clock = clock;
  }

  /** Returns bounded participant-scoped references without choosing a historical relationship. */
  @Override
  @Transactional(readOnly = true)
  public List<Long> findIdsByParticipantsAfter(
      String olderAdultId, String caregiverId, long afterId, int limit) {
    if (afterId < 0 || limit < 1 || limit > 100)
      throw new IllegalArgumentException("invalid monitor scan page");
    return jpaRepository.findIdsByParticipantsAfter(
        olderAdultId, caregiverId, afterId, PageRequest.of(0, limit));
  }

  /** Restores one stored snapshot within its collection-loading transaction. */
  @Override
  @Transactional(readOnly = true)
  public Optional<FamilyMonitor> findById(Long id) {
    return jpaRepository.findById(id).map(FamilyMonitorPersistenceAssembler::toDomain);
  }

  /** Selects a bounded stable page without loading aggregate collections. */
  @Override
  @Transactional(readOnly = true)
  public List<Long> findIdsByOlderAdultIdAfter(String olderAdultId, long afterId, int limit) {
    if (afterId < 0 || limit < 1 || limit > 100)
      throw new IllegalArgumentException("invalid monitor scan page");
    return jpaRepository.findIdsByOlderAdultIdAfter(
        olderAdultId, afterId, PageRequest.of(0, limit));
  }

  /** Refreshes even previously loaded state under the caller's write reservation. */
  @Override
  @Transactional(propagation = Propagation.MANDATORY)
  public Optional<FamilyMonitor> findByIdForUpdate(Long id) {
    return jpaRepository
        .findById(id)
        .map(
            entity -> {
              entities.refresh(entity, LockModeType.PESSIMISTIC_WRITE);
              return FamilyMonitorPersistenceAssembler.toDomain(entity);
            });
  }

  /** Retains stored identities and history while rejecting a snapshot read before another write. */
  @Override
  public FamilyMonitor save(FamilyMonitor monitor) {
    var snapshot = FamilyMonitorPersistenceAssembler.toEntity(monitor);
    if (monitor.getId() == null) {
      if (monitor.getRevision() != null)
        throw new IllegalArgumentException("new follow-up has a stored revision");
      snapshot.setCreatedAt(clock.instant());
      snapshot.setUpdatedAt(snapshot.getCreatedAt());
      entities.persist(snapshot);
      entities.flush();
      return FamilyMonitorPersistenceAssembler.toDomain(snapshot);
    }
    var managed =
        jpaRepository
            .findById(monitor.getId())
            .orElseThrow(() -> new IllegalStateException("stored follow-up is missing"));
    entities.refresh(managed, LockModeType.PESSIMISTIC_WRITE);
    if (!Objects.equals(managed.getRevision(), monitor.getRevision()))
      throw new OptimisticLockException("follow-up changed after the snapshot was read");
    FamilyMonitorPersistenceStateUpdater.apply(managed, snapshot);
    managed.setUpdatedAt(clock.instant());
    entities.lock(managed, LockModeType.PESSIMISTIC_FORCE_INCREMENT);
    entities.flush();
    return FamilyMonitorPersistenceAssembler.toDomain(managed);
  }

  /** Restores exact relationship history without inferring current authorization. */
  @Override
  @Transactional(readOnly = true)
  public Optional<FamilyMonitor> findByCareLinkId(String careLinkId) {
    return jpaRepository
        .findByCareLinkId(careLinkId)
        .map(FamilyMonitorPersistenceAssembler::toDomain);
  }
}
