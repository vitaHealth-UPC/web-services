package com.vitahealth.tata.carelink.domain.model.queries;

import static com.vitahealth.tata.shared.domain.validation.DomainText.requireText;

/** Looks up optional adult profile evidence by its logical identity. */
public record FindOlderAdultProfileByIdQuery(String olderAdultId) {
  /** Requires explicit logical identities independently of transport and persistence. */
  public FindOlderAdultProfileByIdQuery {
    olderAdultId = requireText(olderAdultId, "olderAdultId");
  }
}
