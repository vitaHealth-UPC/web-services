package com.tata.inventoryreplenishment.infrastructure.persistence.jpa.entities;

import jakarta.persistence.*;

import java.time.Instant;

/** One row per confirmed intake already discounted; intake_id is a logical reference to Intake Execution. */
@Entity
@Table(
        name = "inventory_consumptions",
        uniqueConstraints = @UniqueConstraint(name = "uk_inventory_consumptions_intake", columnNames = "intake_id")
)
public class InventoryConsumptionPersistenceEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "intake_id", nullable = false, length = 36) private String intakeId;
    @Column(name = "inventory_id", nullable = false, length = 36) private String inventoryId;
    @Column(name = "consumed_at", nullable = false) private Instant consumedAt;

    protected InventoryConsumptionPersistenceEntity() {}

    public InventoryConsumptionPersistenceEntity(String intakeId, String inventoryId, Instant consumedAt) {
        this.intakeId = intakeId;
        this.inventoryId = inventoryId;
        this.consumedAt = consumedAt;
    }

    public Long getId() { return id; }
    public String getIntakeId() { return intakeId; }
    public String getInventoryId() { return inventoryId; }
    public Instant getConsumedAt() { return consumedAt; }
}
