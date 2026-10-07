package com.tata.omissionescalation.domain.services;

import com.tata.omissionescalation.domain.model.aggregates.OmissionCase;
import com.tata.omissionescalation.domain.model.valueobjects.OmissionCaseStatus;
import java.time.Duration;
import java.time.Instant;

/** Decides whether an omitted case must raise its attention level. */
public class EscalationPolicy {

  public static final Duration ESCALATION_INTERVAL = Duration.ofMinutes(30);

  public boolean shouldEscalate(OmissionCase omissionCase, Instant now) {
    OmissionCaseStatus status = omissionCase.getStatus();
    boolean unresolved =
        status == OmissionCaseStatus.OMITTED || status == OmissionCaseStatus.ESCALATED;
    if (!unresolved || omissionCase.currentLevel().isMax()) {
      return false;
    }
    Instant dueAt = omissionCase.lastActivityAt().plus(ESCALATION_INTERVAL);
    return !now.isBefore(dueAt);
  }
}
