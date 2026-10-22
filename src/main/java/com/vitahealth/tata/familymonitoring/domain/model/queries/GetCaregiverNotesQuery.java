package com.vitahealth.tata.familymonitoring.domain.model.queries;

import static com.vitahealth.tata.shared.domain.validation.DomainText.requireText;

/** Selects notes from the requesting caregiver's follow-up of one adult. */
public record GetCaregiverNotesQuery(String olderAdultId, String caregiverId) {
  /** Requires both participants instead of selecting an arbitrary adult follow-up. */
  public GetCaregiverNotesQuery {
    olderAdultId = requireText(olderAdultId, "olderAdultId");
    caregiverId = requireText(caregiverId, "caregiverId");
  }
}
