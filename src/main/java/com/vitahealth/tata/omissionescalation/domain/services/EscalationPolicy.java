package com.vitahealth.tata.omissionescalation.domain.services;

import com.vitahealth.tata.omissionescalation.domain.model.aggregates.OmissionCase;
import com.vitahealth.tata.omissionescalation.domain.model.valueobjects.OmissionCaseStatus;
import java.time.Duration;
import java.time.Instant;

/** Decides whether an omitted case must raise its attention level. */
public class EscalationPolicy {

  /** Clinical unresolved-case interval, independent of a recipient delivery-channel preference. */
  public static final Duration ESCALATION_INTERVAL = Duration.ofMinutes(30);

  /** Requires an active omission below the maximum level and a fully elapsed activity interval. */
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
