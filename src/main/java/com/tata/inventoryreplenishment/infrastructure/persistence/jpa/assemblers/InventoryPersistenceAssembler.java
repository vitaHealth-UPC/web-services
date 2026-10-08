package com.tata.inventoryreplenishment.infrastructure.persistence.jpa.assemblers;
import com.tata.inventoryreplenishment.domain.model.aggregates.Inventory;
import com.tata.inventoryreplenishment.domain.model.entities.Batch;
import com.tata.inventoryreplenishment.infrastructure.persistence.jpa.entities.BatchPersistenceEntity;
import com.tata.inventoryreplenishment.infrastructure.persistence.jpa.entities.InventoryPersistenceEntity;
import java.util.List;
public final class InventoryPersistenceAssembler {
 private InventoryPersistenceAssembler() {}
    public static Inventory toDomain(InventoryPersistenceEntity entity) {
        return Inventory.rehydrate(
                entity.getId(),
                entity.getMedicationId(),
                entity.getRemainingStock(),
                entity.getReplenishmentThreshold(),
                entity.getBatches().stream()
                        .map(batch -> Batch.rehydrate(batch.getId(), batch.getQuantity(), batch.getRegisteredAt(), batch.getLot()))
                        .toList(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public static List<BatchPersistenceEntity> toEntities(List<Batch> batches) {
        return batches.stream()
                .map(batch -> new BatchPersistenceEntity(batch.id(), batch.quantity(), batch.registeredAt(), batch.lot()))
                .toList();
    }
}
