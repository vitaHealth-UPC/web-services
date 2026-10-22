package com.vitahealth.tata.carelink.domain.services;

import com.vitahealth.tata.carelink.domain.model.aggregates.CareLink;
import com.vitahealth.tata.carelink.domain.model.valueobjects.CareLinkStatus;
import java.time.Instant;
import java.util.Objects;

/** Evaluates recorded consent and historical code use before confirming a care relationship. */
public final class CareLinkConfirmationPolicy {
  /** Allows confirmation after the current transition time, without extending code validity. */
  public boolean canConfirm(CareLink careLink, Instant now) {
    Objects.requireNonNull(careLink);
    Objects.requireNonNull(now);
    return careLink.status() == CareLinkStatus.AWAITING_CONSENT
        && !now.isBefore(careLink.updatedAt())
        && careLink.linkingCode().wasUsedWithinValidity()
        && careLink.consent().isGranted();
  }
}
