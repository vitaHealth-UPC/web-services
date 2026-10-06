package com.tata.carelink.domain.model.aggregates;

import com.tata.carelink.domain.model.valueobjects.EmergencyContact;
import com.tata.carelink.domain.model.valueobjects.OlderAdultBasicData;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class OlderAdultProfile {
    private final String id;
    private final String registeredByCaregiverId;
    private OlderAdultBasicData basicData;
    private EmergencyContact emergencyContact;
    private final Instant createdAt;
    private Instant updatedAt;

    private OlderAdultProfile(
            String id,
            String registeredByCaregiverId,
            OlderAdultBasicData basicData,
            EmergencyContact emergencyContact,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = requireText(id, "id");
        this.registeredByCaregiverId = requireText(registeredByCaregiverId, "registeredByCaregiverId");
        this.basicData = Objects.requireNonNull(basicData);
        this.emergencyContact = emergencyContact;
        this.createdAt = Objects.requireNonNull(createdAt);
        this.updatedAt = Objects.requireNonNull(updatedAt);
    }

    public static OlderAdultProfile register(
            String caregiverId,
            OlderAdultBasicData basicData,
            EmergencyContact emergencyContact,
            Instant now
    ) {
        Objects.requireNonNull(now);
        return new OlderAdultProfile(
                UUID.randomUUID().toString(),
                caregiverId,
                basicData,
                emergencyContact,
                now,
                now
        );
    }

    public static OlderAdultProfile rehydrate(
            String id,
            String caregiverId,
            OlderAdultBasicData basicData,
            EmergencyContact emergencyContact,
            Instant createdAt,
            Instant updatedAt
    ) {
        return new OlderAdultProfile(id, caregiverId, basicData, emergencyContact, createdAt, updatedAt);
    }

    public void updateBasicData(OlderAdultBasicData basicData, Instant now) {
        this.basicData = Objects.requireNonNull(basicData);
        this.updatedAt = Objects.requireNonNull(now);
    }

    public void associateEmergencyContact(EmergencyContact emergencyContact, Instant now) {
        this.emergencyContact = Objects.requireNonNull(emergencyContact);
        this.updatedAt = Objects.requireNonNull(now);
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    public String id() { return id; }
    public String registeredByCaregiverId() { return registeredByCaregiverId; }
    public OlderAdultBasicData basicData() { return basicData; }
    public EmergencyContact emergencyContact() { return emergencyContact; }
    public Instant createdAt() { return createdAt; }
    public Instant updatedAt() { return updatedAt; }
}
