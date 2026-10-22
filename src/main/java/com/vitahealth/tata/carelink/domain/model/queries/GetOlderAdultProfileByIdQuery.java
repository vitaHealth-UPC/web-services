package com.vitahealth.tata.carelink.domain.model.queries;

import static com.vitahealth.tata.shared.domain.validation.DomainText.requireText;

/** Requests current adult profile evidence by its logical identity. */
public record GetOlderAdultProfileByIdQuery(String olderAdultId) {
  /** Requires explicit logical identities independently of transport and persistence. */
  public GetOlderAdultProfileByIdQuery {
    olderAdultId = requireText(olderAdultId, "olderAdultId");
  }
}
