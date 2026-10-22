package com.vitahealth.tata.carelink.application.result;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Application profile projection; birth date and contact data remain independent of UI formatting.
 */
public record OlderAdultProfileResult(
    String id,
    String registeredByCaregiverId,
    String fullName,
    LocalDate birthDate,
    String emergencyContactName,
    String emergencyContactRelationship,
    String emergencyContactPhone,
    Instant createdAt) {
  /** Omits personal and contact data from diagnostic output across context boundaries. */
  @Override
  public String toString() {
    return "OlderAdultProfileResult[id=" + id + ", personalData=REDACTED]";
  }
}
