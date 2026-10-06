package com.tata.treatmentmanagement.infrastructure.persistence.jpa.entities;

import com.tata.treatmentmanagement.domain.model.valueobjects.TreatmentStatus;
import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalTime;

@Entity
@Table(name = "treatment_treatments")
public class TreatmentPersistenceEntity {
    @Id private String id;
    @Column(name = "older_adult_id", nullable = false, length = 36) private String olderAdultId;
    @Column(nullable = false, length = 160) private String name;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private TreatmentStatus status;
    @Column(name = "medication_id", length = 36) private String medicationId;
    @Column(length = 100) private String dose;
    @Column(length = 100) private String frequency;
    @Column(name = "scheduled_time") private LocalTime scheduledTime;
    @Column(length = 500) private String instructions;
    @Column(name = "reminder_lead_minutes") private Integer reminderLeadMinutes;
    @Column(name = "created_at", nullable = false) private Instant createdAt;

    protected TreatmentPersistenceEntity() {}

    public TreatmentPersistenceEntity(
            String id, String olderAdultId, String name, TreatmentStatus status,
            String medicationId, String dose, String frequency, LocalTime scheduledTime,
            String instructions, Integer reminderLeadMinutes, Instant createdAt
    ) {
        this.id = id;
        this.olderAdultId = olderAdultId;
        this.name = name;
        this.status = status;
        this.medicationId = medicationId;
        this.dose = dose;
        this.frequency = frequency;
        this.scheduledTime = scheduledTime;
        this.instructions = instructions;
        this.reminderLeadMinutes = reminderLeadMinutes;
        this.createdAt = createdAt;
    }

    public String getId() { return id; }
    public String getOlderAdultId() { return olderAdultId; }
    public String getName() { return name; }
    public TreatmentStatus getStatus() { return status; }
    public String getMedicationId() { return medicationId; }
    public String getDose() { return dose; }
    public String getFrequency() { return frequency; }
    public LocalTime getScheduledTime() { return scheduledTime; }
    public String getInstructions() { return instructions; }
    public Integer getReminderLeadMinutes() { return reminderLeadMinutes; }
    public Instant getCreatedAt() { return createdAt; }
}
