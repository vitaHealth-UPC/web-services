package com.tata.treatmentmanagement.domain.model.aggregates;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class Medication {
    private final String id;
    private final String olderAdultId;
    private String name;
    private String presentation;
    private boolean active;
    private final Instant createdAt;

    private Medication(
            String id,
            String olderAdultId,
            String name,
            String presentation,
            boolean active,
            Instant createdAt
    ) {
        this.id = Objects.requireNonNull(id);
        this.olderAdultId = requireText(olderAdultId, "olderAdultId");
        this.name = requireText(name, "name");
        this.presentation = requireText(presentation, "presentation");
        this.active = active;
        this.createdAt = Objects.requireNonNull(createdAt);
    }

    public static Medication register(String olderAdultId, String name, String presentation, Instant now) {
        return new Medication(UUID.randomUUID().toString(), olderAdultId, name, presentation, true, now);
    }

    public static Medication rehydrate(
            String id, String olderAdultId, String name, String presentation, boolean active, Instant createdAt
    ) {
        return new Medication(id, olderAdultId, name, presentation, active, createdAt);
    }

    public void update(String name, String presentation) {
        if (!active) throw new IllegalStateException("inactive medication cannot be edited");
        var validatedName = requireText(name, "name");
        var validatedPresentation = requireText(presentation, "presentation");
        this.name = validatedName;
        this.presentation = validatedPresentation;
    }

    public void deactivate() {
        active = false;
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required");
        return value.trim();
    }

    public String id() { return id; }
    public String olderAdultId() { return olderAdultId; }
    public String name() { return name; }
    public String presentation() { return presentation; }
    public boolean active() { return active; }
    public Instant createdAt() { return createdAt; }
}
