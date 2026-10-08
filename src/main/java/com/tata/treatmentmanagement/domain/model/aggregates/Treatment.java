package com.tata.treatmentmanagement.domain.model.aggregates;

import static com.tata.shared.domain.validation.DomainText.requireText;

import com.tata.treatmentmanagement.domain.model.valueobjects.TreatmentRegimen;
import com.tata.treatmentmanagement.domain.model.valueobjects.TreatmentStatus;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class Treatment {
    private final String id;
    private final String olderAdultId;
    private final String name;
    private TreatmentStatus status;
    private TreatmentRegimen regimen;
    private final Instant createdAt;

    private Treatment(
            String id,
            String olderAdultId,
            String name,
            TreatmentStatus status,
            TreatmentRegimen regimen,
            Instant createdAt
    ) {
        this.id = Objects.requireNonNull(id);
        this.olderAdultId = requireText(olderAdultId, "olderAdultId");
        this.name = requireText(name, "name");
        this.status = Objects.requireNonNull(status);
        this.regimen = regimen;
        this.createdAt = Objects.requireNonNull(createdAt);
    }

    public static Treatment create(String olderAdultId, String name, Instant now) {
        return new Treatment(
                UUID.randomUUID().toString(), olderAdultId, name, TreatmentStatus.DRAFT, null, now
        );
    }

    public static Treatment rehydrate(
            String id, String olderAdultId, String name, TreatmentStatus status,
            TreatmentRegimen regimen, Instant createdAt
    ) {
        return new Treatment(id, olderAdultId, name, status, regimen, createdAt);
    }

    public void configure(TreatmentRegimen regimen) {
        this.regimen = Objects.requireNonNull(regimen);
    }

    public void activate() {
        if (regimen == null) throw new IllegalStateException("treatment regimen is incomplete");
        status = TreatmentStatus.ACTIVE;
    }

    public void pause() {
        if (status != TreatmentStatus.ACTIVE) throw new IllegalStateException("only an active treatment can be paused");
        status = TreatmentStatus.PAUSED;
    }

    public void resume() {
        if (status != TreatmentStatus.PAUSED) throw new IllegalStateException("only a paused treatment can be resumed");
        if (regimen == null) throw new IllegalStateException("treatment regimen is incomplete");
        status = TreatmentStatus.ACTIVE;
    }


    public String id() { return id; }
    public String olderAdultId() { return olderAdultId; }
    public String name() { return name; }
    public TreatmentStatus status() { return status; }
    public TreatmentRegimen regimen() { return regimen; }
    public Instant createdAt() { return createdAt; }
}
