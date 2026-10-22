package com.vitahealth.tata.carelink.domain.model.queries;

import static com.vitahealth.tata.shared.domain.validation.DomainText.requireText;

/** Checks the current consented relationship between two logical identities. */
public record CheckCareRelationshipAuthorizationQuery(String caregiverId, String olderAdultId) {
  /** Requires explicit logical identities independently of transport and persistence. */
  public CheckCareRelationshipAuthorizationQuery {
    caregiverId = requireText(caregiverId, "caregiverId");
    olderAdultId = requireText(olderAdultId, "olderAdultId");
  }
}
