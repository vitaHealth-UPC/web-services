package com.vitahealth.tata.carelink.interfaces.rest.resources;

import java.time.Instant;
import java.time.LocalDate;

/** Authorized adult profile response preserving dates and optional contact fields. */
public record OlderAdultProfileResource(
    String id,
    String registeredByCaregiverId,
    String fullName,
    LocalDate birthDate,
    String emergencyContactName,
    String emergencyContactRelationship,
    String emergencyContactPhone,
    Instant createdAt) {
  /** Omits personal data and credentials from diagnostic output. */
  @Override
  public String toString() {
    return "OlderAdultProfileResource[id=" + id + ", personalData=REDACTED]";
  }
}
