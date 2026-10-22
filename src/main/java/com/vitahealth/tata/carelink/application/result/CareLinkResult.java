package com.vitahealth.tata.carelink.application.result;

import com.vitahealth.tata.carelink.domain.model.valueobjects.CareLinkStatus;
import java.time.Instant;

/**
 * Authorized relationship projection preserving lifecycle and consent evidence without JPA types.
 */
public record CareLinkResult(
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
  /** Preserves credential accessors without exposing linking evidence in diagnostics. */
  @Override
  public String toString() {
    return "CareLinkResult[id=" + id + ", status=" + status + ", linkingCode=REDACTED]";
  }
}
