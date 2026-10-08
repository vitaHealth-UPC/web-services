package com.tata.inventoryreplenishment.infrastructure.persistence.jpa.adapters;
import com.tata.inventoryreplenishment.infrastructure.persistence.jpa.assemblers.InventoryPersistenceAssembler;

import com.tata.inventoryreplenishment.domain.model.aggregates.Inventory;
import com.tata.inventoryreplenishment.domain.repositories.InventoryRepository;
import com.tata.inventoryreplenishment.infrastructure.persistence.jpa.entities.InventoryConsumptionPersistenceEntity;
import com.tata.inventoryreplenishment.infrastructure.persistence.jpa.entities.InventoryPersistenceEntity;
import com.tata.inventoryreplenishment.infrastructure.persistence.jpa.repositories.InventoryConsumptionJpaRepository;
import com.tata.inventoryreplenishment.infrastructure.persistence.jpa.repositories.InventoryJpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
@org.springframework.transaction.annotation.Transactional
public class InventoryRepositoryImpl implements InventoryRepository {
    private final InventoryJpaRepository repository;
    private final InventoryConsumptionJpaRepository consumptionRepository;

    public InventoryRepositoryImpl(
            InventoryJpaRepository repository,
            InventoryConsumptionJpaRepository consumptionRepository
    ) {
        this.repository = repository;
        this.consumptionRepository = consumptionRepository;
    }

    /**
     * Updates the managed entity when it already exists, so its version is checked on flush and a
     * concurrent update of the same inventory fails instead of silently overwriting the stock.
     */
    @Override
    public Inventory save(Inventory inventory) {
        var batches = InventoryPersistenceAssembler.toEntities(inventory.batches());
        var entity = repository.findById(inventory.id())
                .map(existing -> {
                    existing.updateStock(inventory.remainingStock(), batches, inventory.updatedAt());
                    return existing;
                })
                .orElseGet(() -> {
                    var created = new InventoryPersistenceEntity(
                            inventory.id(),
                            inventory.medicationId(),
                            inventory.remainingStock(),
                            inventory.replenishmentThreshold(),
                            inventory.createdAt(),
                            inventory.updatedAt()
                    );
                    created.updateStock(inventory.remainingStock(), batches, inventory.updatedAt());
                    return created;
                });
        return InventoryPersistenceAssembler.toDomain(repository.save(entity));
    }

    @Override
    public Optional<Inventory> findByMedicationId(String medicationId) {
        return repository.findByMedicationId(medicationId).map(InventoryPersistenceAssembler::toDomain);
    }

    @Override
    public Optional<Inventory> findByMedicationIdForUpdate(String medicationId) {
        return repository.findByMedicationIdForUpdate(medicationId).map(InventoryPersistenceAssembler::toDomain);
    }

    @Override
    public boolean existsByMedicationId(String medicationId) {
        return repository.existsByMedicationId(medicationId);
    }

    @Override
    public boolean hasConsumed(String intakeId) {
        return consumptionRepository.existsByIntakeId(intakeId);
    }

    @Override
    public void registerConsumption(String intakeId, String inventoryId, Instant consumedAt) {
        consumptionRepository.save(new InventoryConsumptionPersistenceEntity(intakeId, inventoryId, consumedAt));
    }

}
