package com.vitahealth.tata.inventoryreplenishment.infrastructure.persistence.jpa.adapters;

import com.vitahealth.tata.inventoryreplenishment.domain.model.aggregates.Inventory;
import com.vitahealth.tata.inventoryreplenishment.domain.repositories.InventoryRepository;
import com.vitahealth.tata.inventoryreplenishment.infrastructure.persistence.jpa.assemblers.InventoryPersistenceAssembler;
import com.vitahealth.tata.inventoryreplenishment.infrastructure.persistence.jpa.entities.InventoryConsumptionPersistenceEntity;
import com.vitahealth.tata.inventoryreplenishment.infrastructure.persistence.jpa.entities.InventoryPersistenceEntity;
import com.vitahealth.tata.inventoryreplenishment.infrastructure.persistence.jpa.repositories.InventoryConsumptionJpaRepository;
import com.vitahealth.tata.inventoryreplenishment.infrastructure.persistence.jpa.repositories.InventoryJpaRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.OptimisticLockException;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Persists stock and receipt history and refreshes transaction-owned mutation reservations. */
@Repository
@Transactional
public class InventoryRepositoryImpl implements InventoryRepository {
  private final InventoryJpaRepository repository;
  private final EntityManager entities;
  private final InventoryConsumptionJpaRepository consumptionRepository;

  /** Injects inventory storage, unique consumption evidence and the persistence context. */
  public InventoryRepositoryImpl(
      InventoryJpaRepository repository,
      InventoryConsumptionJpaRepository consumptionRepository,
      EntityManager entities) {
    this.repository = repository;
    this.entities = entities;
    this.consumptionRepository = consumptionRepository;
  }

  /** Reserves stored state and rejects any snapshot read before a competing committed change. */
  @Override
  public Inventory save(Inventory inventory) {
    var batches = InventoryPersistenceAssembler.toEntities(inventory.batches());
    if (inventory.revision() == null) {
      var created =
          new InventoryPersistenceEntity(
              inventory.id(),
              inventory.medicationId(),
              inventory.remainingStock(),
              inventory.replenishmentThreshold(),
              inventory.createdAt(),
              inventory.updatedAt());
      created.updateStock(inventory.remainingStock(), batches, inventory.updatedAt());
      entities.persist(created);
      entities.flush();
      return InventoryPersistenceAssembler.toDomain(created);
    }
    var managed =
        repository
            .findById(inventory.id())
            .orElseThrow(() -> new IllegalStateException("stored inventory is missing"));
    entities.refresh(managed, LockModeType.PESSIMISTIC_WRITE);
    if (!Objects.equals(managed.getVersion(), inventory.revision()))
      throw new OptimisticLockException("inventory changed after snapshot was read");
    if (!managed.getMedicationId().equals(inventory.medicationId())
        || managed.getReplenishmentThreshold() != inventory.replenishmentThreshold()
        || !managed.getCreatedAt().equals(inventory.createdAt()))
      throw new IllegalStateException("inventory source identity cannot change");
    managed.updateStock(inventory.remainingStock(), batches, inventory.updatedAt());
    entities.lock(managed, LockModeType.PESSIMISTIC_FORCE_INCREMENT);
    entities.flush();
    return InventoryPersistenceAssembler.toDomain(managed);
  }

  /** Restores stock and complete receipt history without reserving a mutation. */
  @Override
  @Transactional(readOnly = true)
  public Optional<Inventory> findByMedicationId(String medicationId) {
    return repository.findByMedicationId(medicationId).map(InventoryPersistenceAssembler::toDomain);
  }

  /** Refreshes even previously loaded stock under a caller-owned write reservation. */
  @Override
  @Transactional(propagation = Propagation.MANDATORY)
  public Optional<Inventory> findByMedicationIdForUpdate(String medicationId) {
    return repository
        .findByMedicationId(medicationId)
        .map(
            entity -> {
              entities.refresh(entity, LockModeType.PESSIMISTIC_WRITE);
              return InventoryPersistenceAssembler.toDomain(entity);
            });
  }

  /** Checks medication registration without replacing the database uniqueness guarantee. */
  @Override
  @Transactional(readOnly = true)
  public boolean existsByMedicationId(String medicationId) {
    return repository.existsByMedicationId(medicationId);
  }

  /** Checks whether unique decrement evidence already exists for the intake. */
  @Override
  @Transactional(readOnly = true)
  public boolean hasConsumed(String intakeId) {
    return consumptionRepository.existsByIntakeId(intakeId);
  }

  /** Appends one intake decrement marker within the stock mutation transaction. */
  @Override
  public void registerConsumption(String intakeId, String inventoryId, Instant consumedAt) {
    consumptionRepository.save(
        new InventoryConsumptionPersistenceEntity(intakeId, inventoryId, consumedAt));
  }
}
