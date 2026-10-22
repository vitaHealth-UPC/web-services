package com.vitahealth.tata.carelink.domain.model.aggregates;

import static com.vitahealth.tata.shared.domain.validation.DomainText.requireText;

import com.vitahealth.tata.carelink.domain.model.valueobjects.EmergencyContact;
import com.vitahealth.tata.carelink.domain.model.valueobjects.OlderAdultBasicData;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.UUID;

/** Owns adult profile data and contact information independently of care relationship consent. */
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
      Instant updatedAt) {
    this.id = requireText(id, "id");
    this.registeredByCaregiverId = requireText(registeredByCaregiverId, "registeredByCaregiverId");
    this.basicData = Objects.requireNonNull(basicData);
    this.emergencyContact = emergencyContact;
    this.createdAt = Objects.requireNonNull(createdAt);
    this.updatedAt = Objects.requireNonNull(updatedAt);
    if (updatedAt.isBefore(createdAt)) {
      throw new IllegalArgumentException("profile update cannot precede creation");
    }
  }

  /** Registers profile ownership and the initial immutable data snapshots. */
  public static OlderAdultProfile register(
      String caregiverId,
      OlderAdultBasicData basicData,
      EmergencyContact emergencyContact,
      Instant now) {
    Objects.requireNonNull(now);
    requireBirthDateAt(basicData, now);
    return new OlderAdultProfile(
        UUID.randomUUID().toString(), caregiverId, basicData, emergencyContact, now, now);
  }

  /** Restores stored identity and chronology without executing a profile mutation. */
  public static OlderAdultProfile rehydrate(
      String id,
      String caregiverId,
      OlderAdultBasicData basicData,
      EmergencyContact emergencyContact,
      Instant createdAt,
      Instant updatedAt) {
    return new OlderAdultProfile(
        id, caregiverId, basicData, emergencyContact, createdAt, updatedAt);
  }

  /** Replaces basic data only after all proposed state and chronology pass validation. */
  public void updateBasicData(OlderAdultBasicData basicData, Instant now) {
    Objects.requireNonNull(basicData, "basicData");
    requireUpdateTime(now);
    requireBirthDateAt(basicData, now);
    this.basicData = basicData;
    this.updatedAt = now;
  }

  /** Replaces contact information atomically, preserving profile ownership and creation time. */
  public void associateEmergencyContact(EmergencyContact emergencyContact, Instant now) {
    Objects.requireNonNull(emergencyContact, "emergencyContact");
    requireUpdateTime(now);
    this.emergencyContact = emergencyContact;
    this.updatedAt = now;
  }

  private void requireUpdateTime(Instant now) {
    Objects.requireNonNull(now, "now");
    if (now.isBefore(updatedAt))
      throw new IllegalArgumentException("profile update cannot precede current state");
  }

  private static void requireBirthDateAt(OlderAdultBasicData data, Instant now) {
    Objects.requireNonNull(data, "basicData");
    if (data.birthDate().isAfter(LocalDate.ofInstant(now, ZoneOffset.UTC))) {
      throw new IllegalArgumentException("birthDate cannot be in the future");
    }
  }

  /** Returns the stable adult profile identity. */
  public String id() {
    return id;
  }

  /** Returns the caregiver that registered this profile. */
  public String registeredByCaregiverId() {
    return registeredByCaregiverId;
  }

  /** Returns immutable basic profile data. */
  public OlderAdultBasicData basicData() {
    return basicData;
  }

  /** Returns the immutable contact snapshot, or null before association. */
  public EmergencyContact emergencyContact() {
    return emergencyContact;
  }

  /** Returns original profile registration time. */
  public Instant createdAt() {
    return createdAt;
  }

  /** Returns the latest successful domain update time. */
  public Instant updatedAt() {
    return updatedAt;
  }
}
