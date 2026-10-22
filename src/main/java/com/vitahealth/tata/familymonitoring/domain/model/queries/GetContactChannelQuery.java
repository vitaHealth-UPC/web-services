package com.vitahealth.tata.familymonitoring.domain.model.queries;

import static com.vitahealth.tata.shared.domain.validation.DomainText.requireText;

/** Selects adult information within the requesting caregiver's follow-up. */
public record GetContactChannelQuery(String olderAdultId, String caregiverId) {
  /** Requires both logical participants to avoid selecting another relationship. */
  public GetContactChannelQuery {
    olderAdultId = requireText(olderAdultId, "olderAdultId");
    caregiverId = requireText(caregiverId, "caregiverId");
  }
}
