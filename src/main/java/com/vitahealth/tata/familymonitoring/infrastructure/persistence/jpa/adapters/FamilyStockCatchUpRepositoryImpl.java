package com.vitahealth.tata.familymonitoring.infrastructure.persistence.jpa.adapters;

import com.vitahealth.tata.familymonitoring.domain.model.valueobjects.FamilyStockCatchUp;
import com.vitahealth.tata.familymonitoring.domain.repositories.FamilyStockCatchUpRepository;
import com.vitahealth.tata.familymonitoring.infrastructure.persistence.jpa.entities.FamilyStockCatchUpPersistenceEntity;
import com.vitahealth.tata.familymonitoring.infrastructure.persistence.jpa.repositories.FamilyStockCatchUpJpaRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Preserves durable work under caller-owned writes and conditional recovery updates. */
@Repository
@Transactional(propagation = Propagation.MANDATORY)
public class FamilyStockCatchUpRepositoryImpl implements FamilyStockCatchUpRepository {
  private final FamilyStockCatchUpJpaRepository requests;
  private final EntityManager entities;

  /** Injects context-owned work storage and its transaction-scoped persistence context. */
  public FamilyStockCatchUpRepositoryImpl(
      FamilyStockCatchUpJpaRepository requests, EntityManager entities) {
    this.requests = requests;
    this.entities = entities;
  }

  /** Inserts new work without resetting an existing request's cursor or retry evidence. */
  @Override
  public void enqueue(FamilyStockCatchUp request) {
    Objects.requireNonNull(request, "request");
    if (reserved(request.monitorId()).isPresent()) return;
    entities.persist(new FamilyStockCatchUpPersistenceEntity(request));
    entities.flush();
  }

  /** Reads immutable evidence in a short transaction. */
  @Override
  @Transactional(readOnly = true, propagation = Propagation.REQUIRED)
  public Optional<FamilyStockCatchUp> find(long monitorId) {
    requireIdentity(monitorId);
    return requests.findById(monitorId).map(FamilyStockCatchUpPersistenceEntity::toSnapshot);
  }

  /** Refreshes work under its exclusive reservation before processing a medication. */
  @Override
  public Optional<FamilyStockCatchUp> reserve(long monitorId) {
    return reserved(monitorId).map(FamilyStockCatchUpPersistenceEntity::toSnapshot);
  }

  /** Bounds eligible reads at the database and returns detached request evidence. */
  @Override
  @Transactional(readOnly = true, propagation = Propagation.REQUIRED)
  public List<FamilyStockCatchUp> findDueAfter(Instant cutoff, long afterMonitorId, int limit) {
    Objects.requireNonNull(cutoff, "cutoff");
    if (afterMonitorId < 0 || limit < 1 || limit > 100)
      throw new IllegalArgumentException("invalid catch-up page position or limit");
    return requests.findDueAfter(cutoff, afterMonitorId, PageRequest.of(0, limit)).stream()
        .map(FamilyStockCatchUpPersistenceEntity::toSnapshot)
        .toList();
  }

  /** Applies a conditional update without resurrecting completed or advanced work. */
  @Override
  public boolean replace(FamilyStockCatchUp expected, FamilyStockCatchUp replacement) {
    Objects.requireNonNull(expected, "expected");
    Objects.requireNonNull(replacement, "replacement");
    var current = reserved(expected.monitorId());
    if (current.isEmpty() || !current.get().toSnapshot().equals(expected)) return false;
    current.get().apply(replacement);
    entities.flush();
    return true;
  }

  /** Removes exactly observed work atomically with the caller's final projection. */
  @Override
  public boolean complete(FamilyStockCatchUp expected) {
    Objects.requireNonNull(expected, "expected");
    var current = reserved(expected.monitorId());
    if (current.isEmpty() || !current.get().toSnapshot().equals(expected)) return false;
    entities.remove(current.get());
    entities.flush();
    return true;
  }

  private Optional<FamilyStockCatchUpPersistenceEntity> reserved(long monitorId) {
    requireIdentity(monitorId);
    return requests
        .findById(monitorId)
        .map(
            work -> {
              entities.refresh(work, LockModeType.PESSIMISTIC_WRITE);
              return work;
            });
  }

  private static void requireIdentity(long monitorId) {
    if (monitorId <= 0) throw new IllegalArgumentException("monitor identity must be positive");
  }
}
