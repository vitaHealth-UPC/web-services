package com.tata.inventoryreplenishment.infrastructure.persistence.jpa.entities;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * medication_id is a logical reference to Treatment Management: unique, but without a foreign key.
 * The version column rejects concurrent updates of the same inventory (optimistic locking).
 */
@Entity
@Table(
        name = "inventories",
        uniqueConstraints = @UniqueConstraint(name = "uk_inventories_medication", columnNames = "medication_id")
)
public class InventoryPersistenceEntity {
    @Id private String id;
    @Column(name = "medication_id", nullable = false, length = 36) private String medicationId;
    @Column(name = "remaining_stock", nullable = false) private int remainingStock;
    @Column(name = "replenishment_threshold", nullable = false) private int replenishmentThreshold;
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "inventory_id", nullable = false)
    @OrderBy("registeredAt ASC")
    private List<BatchPersistenceEntity> batches = new ArrayList<>();
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @Version private Long version;

    protected InventoryPersistenceEntity() {}

    public InventoryPersistenceEntity(
            String id,
            String medicationId,
            int remainingStock,
            int replenishmentThreshold,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = id;
        this.medicationId = medicationId;
        this.remainingStock = remainingStock;
        this.replenishmentThreshold = replenishmentThreshold;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /** Batches are immutable facts: only the ones not yet persisted are appended. */
    public void updateStock(int remainingStock, List<BatchPersistenceEntity> currentBatches, Instant updatedAt) {
        this.remainingStock = remainingStock;
        var persistedIds = batches.stream().map(BatchPersistenceEntity::getId).toList();
        currentBatches.stream()
                .filter(batch -> !persistedIds.contains(batch.getId()))
                .forEach(batches::add);
        this.updatedAt = updatedAt;
    }

    public String getId() { return id; }
    public String getMedicationId() { return medicationId; }
    public int getRemainingStock() { return remainingStock; }
    public int getReplenishmentThreshold() { return replenishmentThreshold; }
    public List<BatchPersistenceEntity> getBatches() { return batches; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
