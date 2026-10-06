package com.tata.treatmentmanagement.infrastructure.persistence.jpa.entities;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "treatment_medications")
public class MedicationPersistenceEntity {
    @Id private String id;
    @Column(name = "older_adult_id", nullable = false, length = 36) private String olderAdultId;
    @Column(nullable = false, length = 160) private String name;
    @Column(nullable = false, length = 160) private String presentation;
    @Column(nullable = false) private boolean active;
    @Column(name = "created_at", nullable = false) private Instant createdAt;

    protected MedicationPersistenceEntity() {}

    public MedicationPersistenceEntity(
            String id, String olderAdultId, String name, String presentation, boolean active, Instant createdAt
    ) {
        this.id = id;
        this.olderAdultId = olderAdultId;
        this.name = name;
        this.presentation = presentation;
        this.active = active;
        this.createdAt = createdAt;
    }

    public String getId() { return id; }
    public String getOlderAdultId() { return olderAdultId; }
    public String getName() { return name; }
    public String getPresentation() { return presentation; }
    public boolean isActive() { return active; }
    public Instant getCreatedAt() { return createdAt; }
}
