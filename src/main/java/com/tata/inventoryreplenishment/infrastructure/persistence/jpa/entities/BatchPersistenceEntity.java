package com.tata.inventoryreplenishment.infrastructure.persistence.jpa.entities;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "inventory_batches")
public class BatchPersistenceEntity {
    @Id private String id;
    @Column(nullable = false) private int quantity;
    @Column(name = "registered_at", nullable = false) private Instant registeredAt;

    protected BatchPersistenceEntity() {}

    public BatchPersistenceEntity(String id, int quantity, Instant registeredAt) {
        this.id = id;
        this.quantity = quantity;
        this.registeredAt = registeredAt;
    }

    public String getId() { return id; }
    public int getQuantity() { return quantity; }
    public Instant getRegisteredAt() { return registeredAt; }
}
