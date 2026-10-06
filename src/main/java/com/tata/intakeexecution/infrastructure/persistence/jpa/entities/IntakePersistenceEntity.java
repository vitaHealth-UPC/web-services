package com.tata.intakeexecution.infrastructure.persistence.jpa.entities;

import com.tata.intakeexecution.domain.model.valueobjects.IntakeStatus;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "intake_intakes")
public class IntakePersistenceEntity {
    @Id
    private String id;

    @Column(name = "treatment_id", nullable = false, length = 36)
    private String treatmentId;

    @Column(name = "medication_id", nullable = false, length = 36)
    private String medicationId;

    @Column(name = "older_adult_id", nullable = false, length = 36)
    private String olderAdultId;

    @Column(name = "medication_name", nullable = false, length = 160)
    private String medicationName;

    @Column(nullable = false, length = 100)
    private String dose;

    @Column(length = 500)
    private String instructions;

    @Column(name = "scheduled_at", nullable = false)
    private Instant scheduledAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private IntakeStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected IntakePersistenceEntity() {}

    public IntakePersistenceEntity(
            String id,
            String treatmentId,
            String medicationId,
            String olderAdultId,
            String medicationName,
            String dose,
            String instructions,
            Instant scheduledAt,
            IntakeStatus status,
            Instant createdAt
    ) {
        this.id = id;
        this.treatmentId = treatmentId;
        this.medicationId = medicationId;
        this.olderAdultId = olderAdultId;
        this.medicationName = medicationName;
        this.dose = dose;
        this.instructions = instructions;
        this.scheduledAt = scheduledAt;
        this.status = status;
        this.createdAt = createdAt;
    }

    public String getId() { return id; }
    public String getTreatmentId() { return treatmentId; }
    public String getMedicationId() { return medicationId; }
    public String getOlderAdultId() { return olderAdultId; }
    public String getMedicationName() { return medicationName; }
    public String getDose() { return dose; }
    public String getInstructions() { return instructions; }
    public Instant getScheduledAt() { return scheduledAt; }
    public IntakeStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
}
