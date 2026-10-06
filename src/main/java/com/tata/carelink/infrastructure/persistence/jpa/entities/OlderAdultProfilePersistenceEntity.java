package com.tata.carelink.infrastructure.persistence.jpa.entities;

import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "older_adult_profiles")
public class OlderAdultProfilePersistenceEntity {
    @Id private String id;
    @Column(name = "registered_by_caregiver_id", nullable = false) private String registeredByCaregiverId;
    @Column(name = "full_name", nullable = false, length = 160) private String fullName;
    @Column(name = "birth_date", nullable = false) private LocalDate birthDate;
    @Column(name = "emergency_contact_name", length = 160) private String emergencyContactName;
    @Column(name = "emergency_contact_relationship", length = 80) private String emergencyContactRelationship;
    @Column(name = "emergency_contact_phone", length = 40) private String emergencyContactPhone;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    protected OlderAdultProfilePersistenceEntity() {}

    public OlderAdultProfilePersistenceEntity(
            String id,
            String registeredByCaregiverId,
            String fullName,
            LocalDate birthDate,
            String emergencyContactName,
            String emergencyContactRelationship,
            String emergencyContactPhone,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = id;
        this.registeredByCaregiverId = registeredByCaregiverId;
        this.fullName = fullName;
        this.birthDate = birthDate;
        this.emergencyContactName = emergencyContactName;
        this.emergencyContactRelationship = emergencyContactRelationship;
        this.emergencyContactPhone = emergencyContactPhone;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getId() { return id; }
    public String getRegisteredByCaregiverId() { return registeredByCaregiverId; }
    public String getFullName() { return fullName; }
    public LocalDate getBirthDate() { return birthDate; }
    public String getEmergencyContactName() { return emergencyContactName; }
    public String getEmergencyContactRelationship() { return emergencyContactRelationship; }
    public String getEmergencyContactPhone() { return emergencyContactPhone; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
