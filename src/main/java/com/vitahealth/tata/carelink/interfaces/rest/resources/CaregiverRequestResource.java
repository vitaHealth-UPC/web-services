package com.vitahealth.tata.carelink.interfaces.rest.resources;

import java.time.Instant;

/** Another caregiver waiting for the adult's decision. */
public record CaregiverRequestResource(
    String careLinkId, String caregiverId, String caregiverName, Instant requestedAt) {
  /** Keeps personal data out of diagnostic output. */
  @Override
  public String toString() {
    return "CaregiverRequestResource[careLinkId=" + careLinkId + ", personalData=REDACTED]";
  }
}
