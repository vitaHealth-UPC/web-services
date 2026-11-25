package com.vitahealth.tata.carelink.application.result;

import java.time.Instant;

/** Another caregiver waiting for the adult to accept them. */
public record CaregiverRequestResult(
    String careLinkId, String caregiverId, String caregiverName, Instant requestedAt) {
  /** Keeps personal data out of diagnostic output. */
  @Override
  public String toString() {
    return "CaregiverRequestResult[careLinkId=" + careLinkId + ", personalData=REDACTED]";
  }
}
