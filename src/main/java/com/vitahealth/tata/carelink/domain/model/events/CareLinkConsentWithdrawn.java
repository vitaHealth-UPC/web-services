package com.vitahealth.tata.carelink.domain.model.events;

import static com.vitahealth.tata.shared.domain.validation.DomainText.requireText;

import java.time.Instant;
import java.util.Objects;

/** Records the original approval and adult withdrawal so consumers retain consent history. */
public record CareLinkConsentWithdrawn(
    String careLinkId,
    String caregiverId,
    String olderAdultId,
    Instant approvedAt,
    Instant withdrawnAt) {
  /** Requires complete decision evidence in chronological order. */
  public CareLinkConsentWithdrawn {
    careLinkId = requireText(careLinkId, "careLinkId");
    caregiverId = requireText(caregiverId, "caregiverId");
    olderAdultId = requireText(olderAdultId, "olderAdultId");
    Objects.requireNonNull(approvedAt, "approvedAt");
    Objects.requireNonNull(withdrawnAt, "withdrawnAt");
    if (withdrawnAt.isBefore(approvedAt))
      throw new IllegalArgumentException("withdrawal cannot precede approval");
  }
}
