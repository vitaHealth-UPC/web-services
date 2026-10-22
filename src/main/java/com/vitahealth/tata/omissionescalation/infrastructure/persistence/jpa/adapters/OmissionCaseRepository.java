package com.vitahealth.tata.omissionescalation.infrastructure.persistence.jpa.adapters;

import com.vitahealth.tata.omissionescalation.domain.model.aggregates.OmissionCase;
import com.vitahealth.tata.omissionescalation.domain.model.valueobjects.OmissionCaseStatus;
import com.vitahealth.tata.omissionescalation.domain.repositories.IOmissionCaseRepository;
import com.vitahealth.tata.omissionescalation.infrastructure.persistence.jpa.assemblers.OmissionCasePersistenceAssembler;
import com.vitahealth.tata.omissionescalation.infrastructure.persistence.jpa.repositories.OmissionCaseJpaRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Maps stored omission evidence and context-owned work selections inside repository transactions.
 */
@Repository
@Transactional
public class OmissionCaseRepository implements IOmissionCaseRepository {

  private final OmissionCaseJpaRepository jpaRepository;
  private final EntityManager entities;
  private final Clock clock;

  /** Injects storage and the persistence context used to refresh reserved evidence. */
  public OmissionCaseRepository(
      OmissionCaseJpaRepository jpaRepository, EntityManager entities, Clock clock) {
    this.jpaRepository = jpaRepository;
    this.entities = entities;
    this.clock = Objects.requireNonNull(clock, "clock");
  }

  /**
   * Persists new evidence or updates the reserved managed row without merging a detached snapshot.
   */
  @Override
  public OmissionCase save(OmissionCase omissionCase) {
    var replacement = OmissionCasePersistenceAssembler.toEntity(omissionCase);
    if (omissionCase.getId() == null) {
      var now = clock.instant();
      replacement.setCreatedAt(now);
      replacement.setUpdatedAt(now);
      entities.persist(replacement);
      entities.flush();
      return OmissionCasePersistenceAssembler.toDomain(replacement);
    }
    var managed =
        jpaRepository
            .findByIdForUpdate(omissionCase.getId())
            .orElseThrow(() -> new IllegalStateException("stored omission does not exist"));
    entities.refresh(managed, LockModeType.PESSIMISTIC_WRITE);
    managed.applyMutableState(replacement);
    managed.setUpdatedAt(clock.instant());
    entities.flush();
    return OmissionCasePersistenceAssembler.toDomain(managed);
  }

  @Override
  public Optional<OmissionCase> findById(Long id) {
    return jpaRepository.findById(id).map(OmissionCasePersistenceAssembler::toDomain);
  }

  /** Requires the decision transaction and refreshes already managed state under its row lock. */
  @Override
  @Transactional(propagation = Propagation.MANDATORY)
  public Optional<OmissionCase> findByIdForUpdate(Long id) {
    return jpaRepository
        .findByIdForUpdate(id)
        .map(
            entity -> {
              entities.refresh(entity, LockModeType.PESSIMISTIC_WRITE);
              return OmissionCasePersistenceAssembler.toDomain(entity);
            });
  }

  @Override
  public Optional<OmissionCase> findByIntakeId(String intakeId) {
    return jpaRepository.findByIntakeId(intakeId).map(OmissionCasePersistenceAssembler::toDomain);
  }

  /** Selects a bounded page of expired pending references without loading their history. */
  @Override
  @Transactional(readOnly = true)
  public List<Long> findExpiredPendingIdsAfter(Instant now, long afterId, int limit) {
    if (now == null) throw new IllegalArgumentException("evaluation time is required");
    return jpaRepository.findExpiredPendingIdsAfter(
        OmissionCaseStatus.PENDING, now, afterId, page(afterId, limit));
  }

  /** Returns a bounded page of missing-alert references without loading aggregate collections. */
  @Override
  @Transactional(readOnly = true)
  public List<Long> findActiveCaseIdsWithoutAlertAfter(long afterId, int limit) {
    return jpaRepository.findActiveCaseIdsWithoutAlertAfter(
        afterId,
        List.of(OmissionCaseStatus.OMITTED, OmissionCaseStatus.ESCALATED),
        page(afterId, limit));
  }

  /** Selects a bounded page of active references for independent decision transactions. */
  @Override
  @Transactional(readOnly = true)
  public List<Long> findActiveCaseIdsAfter(long afterId, int limit) {
    return jpaRepository.findActiveCaseIdsAfter(
        List.of(OmissionCaseStatus.OMITTED, OmissionCaseStatus.ESCALATED),
        afterId,
        page(afterId, limit));
  }

  private static PageRequest page(long afterId, int limit) {
    if (afterId < 0 || limit < 1 || limit > 100)
      throw new IllegalArgumentException(
          "recovery requires a nonnegative cursor and a page size from 1 to 100");
    return PageRequest.of(0, limit);
  }
}
