package com.vitahealth.tata.carelink.infrastructure.persistence.jpa.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

/** Stores adult profile data without coupling relationship consent to profile ownership. */
@Entity
@Table(name = "older_adult_profiles")
public class OlderAdultProfilePersistenceEntity {
  @Id private String id;

  @Column(name = "registered_by_caregiver_id", nullable = false)
  private String registeredByCaregiverId;

  @Column(name = "full_name", nullable = false, length = 160)
  private String fullName;

  @Column(name = "birth_date", nullable = false)
  private LocalDate birthDate;

  @Column(name = "emergency_contact_name", length = 160)
  private String emergencyContactName;

  @Column(name = "emergency_contact_relationship", length = 80)
  private String emergencyContactRelationship;

  @Column(name = "emergency_contact_phone", length = 40)
  private String emergencyContactPhone;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  /** Constructor required by JPA hydration. */
  protected OlderAdultProfilePersistenceEntity() {}

  /** Creates the persistence representation with the domain's stable identity and chronology. */
  public OlderAdultProfilePersistenceEntity(
      String id,
      String registeredByCaregiverId,
      String fullName,
      LocalDate birthDate,
      String emergencyContactName,
      String emergencyContactRelationship,
      String emergencyContactPhone,
      Instant createdAt,
      Instant updatedAt) {
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

  /** Updates mutable profile fields while preserving identity, owner and creation evidence. */
  public void updateFrom(OlderAdultProfilePersistenceEntity state) {
    Objects.requireNonNull(state, "state");
    if (!Objects.equals(id, state.id)
        || !Objects.equals(registeredByCaregiverId, state.registeredByCaregiverId)
        || !Objects.equals(createdAt, state.createdAt)) {
      throw new IllegalArgumentException(
          "profile identity, registering caregiver and creation evidence cannot change");
    }
    if (state.updatedAt.isBefore(updatedAt))
      throw new IllegalArgumentException("profile update cannot precede persisted state");
    fullName = state.fullName;
    birthDate = state.birthDate;
    emergencyContactName = state.emergencyContactName;
    emergencyContactRelationship = state.emergencyContactRelationship;
    emergencyContactPhone = state.emergencyContactPhone;
    updatedAt = state.updatedAt;
  }

  /** Returns stable profile identity. */
  public String getId() {
    return id;
  }

  /** Returns the original registering caregiver. */
  public String getRegisteredByCaregiverId() {
    return registeredByCaregiverId;
  }

  /** Returns the stored normalized adult name. */
  public String getFullName() {
    return fullName;
  }

  /** Returns birth date independent of server clock at hydration time. */
  public LocalDate getBirthDate() {
    return birthDate;
  }

  /** Returns associated contact name, or null when no contact exists. */
  public String getEmergencyContactName() {
    return emergencyContactName;
  }

  /** Returns contact relationship, or null before association. */
  public String getEmergencyContactRelationship() {
    return emergencyContactRelationship;
  }

  /** Returns contact phone information, or null before association. */
  public String getEmergencyContactPhone() {
    return emergencyContactPhone;
  }

  /** Returns the immutable domain registration time. */
  public Instant getCreatedAt() {
    return createdAt;
  }

  /** Returns the latest successfully persisted domain update time. */
  public Instant getUpdatedAt() {
    return updatedAt;
  }
}
