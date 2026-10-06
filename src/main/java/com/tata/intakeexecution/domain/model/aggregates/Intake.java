package com.tata.intakeexecution.domain.model.aggregates;

import com.tata.intakeexecution.domain.model.valueobjects.IntakeStatus;
import com.tata.intakeexecution.domain.model.valueobjects.MedicationSnapshot;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class Intake {
    private final String id;
    private final String treatmentId;
    private final String medicationId;
    private final String olderAdultId;
    private final MedicationSnapshot medication;
    private final Instant scheduledAt;
    private IntakeStatus status;
    private final Instant createdAt;

    private Intake(
            String id,
            String treatmentId,
            String medicationId,
            String olderAdultId,
            MedicationSnapshot medication,
            Instant scheduledAt,
            IntakeStatus status,
            Instant createdAt
    ) {
        this.id = requireText(id, "id");
        this.treatmentId = requireText(treatmentId, "treatmentId");
        this.medicationId = requireText(medicationId, "medicationId");
        this.olderAdultId = requireText(olderAdultId, "olderAdultId");
        this.medication = Objects.requireNonNull(medication);
        this.scheduledAt = Objects.requireNonNull(scheduledAt);
        this.status = Objects.requireNonNull(status);
        this.createdAt = Objects.requireNonNull(createdAt);
    }

    public static Intake createScheduled(
            String treatmentId,
            String medicationId,
            String olderAdultId,
            MedicationSnapshot medication,
            Instant scheduledAt,
            Instant now
    ) {
        return new Intake(
                UUID.randomUUID().toString(),
                treatmentId,
                medicationId,
                olderAdultId,
                medication,
                scheduledAt,
                IntakeStatus.PENDING,
                now
        );
    }

    public static Intake rehydrate(
            String id,
            String treatmentId,
            String medicationId,
            String olderAdultId,
            MedicationSnapshot medication,
            Instant scheduledAt,
            IntakeStatus status,
            Instant createdAt
    ) {
        return new Intake(id, treatmentId, medicationId, olderAdultId, medication, scheduledAt, status, createdAt);
    }

    public boolean isPending() {
        return status == IntakeStatus.PENDING;
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required");
        return value.trim();
    }

    public String id() { return id; }
    public String treatmentId() { return treatmentId; }
    public String medicationId() { return medicationId; }
    public String olderAdultId() { return olderAdultId; }
    public MedicationSnapshot medication() { return medication; }
    public Instant scheduledAt() { return scheduledAt; }
    public IntakeStatus status() { return status; }
    public Instant createdAt() { return createdAt; }
}
