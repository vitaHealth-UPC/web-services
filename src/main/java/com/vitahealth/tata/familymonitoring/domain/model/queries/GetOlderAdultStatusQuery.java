package com.vitahealth.tata.familymonitoring.domain.model.queries;

import static com.vitahealth.tata.shared.domain.validation.DomainText.requireText;

/** Selects adult information within the requesting caregiver's follow-up. */
public record GetOlderAdultStatusQuery(String olderAdultId, String caregiverId) {
  /** Requires both logical participants to avoid selecting another relationship. */
  public GetOlderAdultStatusQuery {
    olderAdultId = requireText(olderAdultId, "olderAdultId");
    caregiverId = requireText(caregiverId, "caregiverId");
  }
}
