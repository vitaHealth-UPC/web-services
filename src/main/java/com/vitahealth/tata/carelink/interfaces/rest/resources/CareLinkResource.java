package com.vitahealth.tata.carelink.interfaces.rest.resources;

import com.vitahealth.tata.carelink.domain.model.valueobjects.CareLinkStatus;
import java.time.Instant;

/** Authorized relationship evidence with optional issued session credentials. */
public record CareLinkResource(
    String id,
    String caregiverId,
    String olderAdultId,
    CareLinkStatus status,
    String linkingCode,
    Instant codeExpiresAt,
    Instant codeUsedAt,
    boolean consentGranted,
    Instant consentRecordedAt,
    Instant confirmedAt,
    String accessToken,
    Instant expiresAt) {
  /** Creates a lookup response without issuing a session. */
  public CareLinkResource(
      String id,
      String caregiverId,
      String olderAdultId,
      CareLinkStatus status,
      String linkingCode,
      Instant codeExpiresAt,
      Instant codeUsedAt,
      boolean consentGranted,
      Instant consentRecordedAt,
      Instant confirmedAt) {
    this(
        id,
        caregiverId,
        olderAdultId,
        status,
        linkingCode,
        codeExpiresAt,
        codeUsedAt,
        consentGranted,
        consentRecordedAt,
        confirmedAt,
        null,
        null);
  }

  /** Omits personal data and credentials from diagnostic output. */
  @Override
  public String toString() {
    return "CareLinkResource[id=" + id + ", status=" + status + ", credentials=REDACTED]";
  }
}
