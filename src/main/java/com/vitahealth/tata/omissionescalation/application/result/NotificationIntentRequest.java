package com.vitahealth.tata.omissionescalation.application.result;

import com.vitahealth.tata.omissionescalation.domain.model.valueobjects.NotificationKind;
import java.time.Instant;

/** Requests durable delivery work for one source case without claiming provider acceptance. */
public record NotificationIntentRequest(
    long omissionCaseId, NotificationKind kind, Instant requestedAt) {
  /** Requires an existing logical source and explicit request time. */
  public NotificationIntentRequest {
    if (omissionCaseId < 1 || kind == null || requestedAt == null)
      throw new IllegalArgumentException("notification intent source, kind and time are required");
  }
}
