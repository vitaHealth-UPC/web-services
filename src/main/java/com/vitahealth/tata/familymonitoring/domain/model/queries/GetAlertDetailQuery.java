package com.vitahealth.tata.familymonitoring.domain.model.queries;

import static com.vitahealth.tata.shared.domain.validation.DomainText.requireText;

/** Identifies one alert within the requesting caregiver's follow-up of an adult. */
public record GetAlertDetailQuery(String olderAdultId, String caregiverId, Long alertId) {
  /** Requires both participants and a positive stored alert identity. */
  public GetAlertDetailQuery {
    olderAdultId = requireText(olderAdultId, "olderAdultId");
    caregiverId = requireText(caregiverId, "caregiverId");
    if (alertId == null || alertId < 1)
      throw new IllegalArgumentException("positive alert id is required");
  }
}
