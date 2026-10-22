package com.vitahealth.tata.familymonitoring.domain.model.queries;

import static com.vitahealth.tata.shared.domain.validation.DomainText.requireText;

/** Selects adult information within the requesting caregiver's follow-up. */
public record GetRecentIntakeHistoryQuery(String olderAdultId, String caregiverId, int days) {
  /** Requires both logical participants to avoid selecting another relationship. */
  public GetRecentIntakeHistoryQuery {
    olderAdultId = requireText(olderAdultId, "olderAdultId");
    caregiverId = requireText(caregiverId, "caregiverId");
  }
}
