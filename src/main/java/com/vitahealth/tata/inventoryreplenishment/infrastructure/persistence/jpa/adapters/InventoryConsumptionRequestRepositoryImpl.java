package com.vitahealth.tata.inventoryreplenishment.infrastructure.persistence.jpa.adapters;

import static com.vitahealth.tata.shared.domain.validation.DomainText.requireText;

import com.vitahealth.tata.inventoryreplenishment.domain.model.commands.RequestInventoryConsumptionCommand;
import com.vitahealth.tata.inventoryreplenishment.domain.model.valueobjects.PendingInventoryConsumption;
import com.vitahealth.tata.inventoryreplenishment.domain.repositories.InventoryConsumptionRequestRepository;
import com.vitahealth.tata.inventoryreplenishment.infrastructure.persistence.jpa.entities.InventoryConsumptionRequestPersistenceEntity;
import com.vitahealth.tata.inventoryreplenishment.infrastructure.persistence.jpa.repositories.InventoryConsumptionRequestJpaRepository;
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

/** Maintains durable intent under caller-owned transactions without rewriting source evidence. */
@Repository
@Transactional(propagation = Propagation.MANDATORY)
public class InventoryConsumptionRequestRepositoryImpl
    implements InventoryConsumptionRequestRepository {
  private final InventoryConsumptionRequestJpaRepository requests;
  private final EntityManager entities;

  /** Injects request storage and the reservation persistence context. */
  public InventoryConsumptionRequestRepositoryImpl(
      InventoryConsumptionRequestJpaRepository requests, EntityManager entities) {
    this.requests = requests;
    this.entities = entities;
  }

  /** Inserts new intent or checks an identical replay without resetting retry state. */
  @Override
  public void enqueue(PendingInventoryConsumption request) {
    Objects.requireNonNull(request, "request");
    var existing = reserved(request.intakeId());
    if (existing.isPresent()) {
      var command =
          new RequestInventoryConsumptionCommand(
              request.intakeId(),
              request.medicationId(),
              request.olderAdultId(),
              request.confirmedAt());
      if (!existing.get().toSnapshot().matches(command))
        throw new IllegalStateException("conflicting inventory consumption source evidence");
      return;
    }
    entities.persist(new InventoryConsumptionRequestPersistenceEntity(request));
    entities.flush();
  }

  /** Reads a detached snapshot in a short transaction. */
  @Override
  @Transactional(readOnly = true, propagation = Propagation.REQUIRED)
  public Optional<PendingInventoryConsumption> findByIntakeId(String intakeId) {
    return requests
        .findById(requireText(intakeId, "intakeId"))
        .map(InventoryConsumptionRequestPersistenceEntity::toSnapshot);
  }

  /** Refreshes current request state under the caller processing reservation. */
  @Override
  public Optional<PendingInventoryConsumption> findByIntakeIdForUpdate(String intakeId) {
    return reserved(intakeId).map(InventoryConsumptionRequestPersistenceEntity::toSnapshot);
  }

  /** Reads bounded eligible evidence without reserving the batch for mutation. */
  @Override
  @Transactional(readOnly = true, propagation = Propagation.REQUIRED)
  public List<PendingInventoryConsumption> findDueAfter(
      Instant cutoff, PendingInventoryConsumption after, int limit) {
    Objects.requireNonNull(cutoff, "cutoff");
    if (limit < 1 || limit > 100) throw new IllegalArgumentException("invalid recovery page size");
    var page = PageRequest.of(0, limit);
    var found =
        after == null
            ? requests.findDue(cutoff, page)
            : requests.findDueAfter(cutoff, after.nextAttemptAt(), after.intakeId(), page);
    return found.stream().map(InventoryConsumptionRequestPersistenceEntity::toSnapshot).toList();
  }

  /** Removes pending intent only in the surrounding successful processing transaction. */
  @Override
  public void complete(String intakeId) {
    reserved(intakeId).ifPresent(entities::remove);
    entities.flush();
  }

  /** Ignores stale failure callbacks and never recreates a completed request. */
  @Override
  public void recordRetry(PendingInventoryConsumption expected, Instant nextAttemptAt) {
    Objects.requireNonNull(expected, "expected");
    Objects.requireNonNull(nextAttemptAt, "nextAttemptAt");
    reserved(expected.intakeId())
        .ifPresent(
            entity -> {
              if (entity.toSnapshot().equals(expected)) entity.retryAt(nextAttemptAt);
            });
    entities.flush();
  }

  private Optional<InventoryConsumptionRequestPersistenceEntity> reserved(String intakeId) {
    return requests
        .findById(requireText(intakeId, "intakeId"))
        .map(
            entity -> {
              entities.refresh(entity, LockModeType.PESSIMISTIC_WRITE);
              return entity;
            });
  }
}
