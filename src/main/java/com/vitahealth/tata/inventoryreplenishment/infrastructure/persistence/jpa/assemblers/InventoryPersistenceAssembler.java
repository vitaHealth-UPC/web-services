package com.vitahealth.tata.inventoryreplenishment.infrastructure.persistence.jpa.assemblers;

import com.vitahealth.tata.inventoryreplenishment.domain.model.aggregates.Inventory;
import com.vitahealth.tata.inventoryreplenishment.domain.model.entities.Batch;
import com.vitahealth.tata.inventoryreplenishment.infrastructure.persistence.jpa.entities.BatchPersistenceEntity;
import com.vitahealth.tata.inventoryreplenishment.infrastructure.persistence.jpa.entities.InventoryPersistenceEntity;
import java.util.List;

/**
 * Translates managed stock and immutable receipts to domain snapshots carrying observed revision.
 */
public final class InventoryPersistenceAssembler {
  private InventoryPersistenceAssembler() {}

  /** Restores complete evidence without publishing historical events. */
  public static Inventory toDomain(InventoryPersistenceEntity entity) {
    return Inventory.rehydrate(
        entity.getId(),
        entity.getMedicationId(),
        entity.getRemainingStock(),
        entity.getReplenishmentThreshold(),
        entity.getBatches().stream()
            .map(
                batch ->
                    Batch.rehydrate(
                        batch.getId(),
                        batch.getQuantity(),
                        batch.getRegisteredAt(),
                        batch.getLot()))
            .toList(),
        entity.getCreatedAt(),
        entity.getUpdatedAt(),
        entity.getVersion());
  }

  /** Copies receipt facts for persistence while retaining original identifiers and timestamps. */
  public static List<BatchPersistenceEntity> toEntities(List<Batch> batches) {
    return batches.stream()
        .map(
            batch ->
                new BatchPersistenceEntity(
                    batch.id(), batch.quantity(), batch.registeredAt(), batch.lot()))
        .toList();
  }
}
