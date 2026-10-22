package com.vitahealth.tata.familymonitoring.domain.model.queries;

import static com.vitahealth.tata.shared.domain.validation.DomainText.requireText;

/** Selects private reminders for the owner obtained from the authenticated adult session. */
public record GetPersonalNotesQuery(String olderAdultId) {
  /** Requires the logical adult identity before querying persistence. */
  public GetPersonalNotesQuery {
    olderAdultId = requireText(olderAdultId, "olderAdultId");
  }
}
